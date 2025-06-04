(ns calorias-api.db
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(def arquivo "dados.json")

(def banco (atom {}))
(def usuario-ativo (atom nil))
(def fmt (DateTimeFormatter/ofPattern "yyyy-MM-dd"))

(defn salvar []
  (try
    (spit arquivo (json/generate-string {:banco @banco :ultimo-usuario-ativo (:username @usuario-ativo)}))
    true
    (catch Exception e
      {:error (str "Erro crítico ao salvar dados em " arquivo ": " (.getMessage e))})))

(defn carregar []
  (if (.exists (io/file arquivo))
    (try
      (let [dados-salvos (json/parse-string (slurp arquivo) true)]
        (reset! banco (:banco dados-salvos))
        (if-let [ultimo-username (:ultimo-usuario-ativo dados-salvos)]
          (if-let [user-data (get @banco ultimo-username)]
            (reset! usuario-ativo (:dados user-data))
            nil)
          nil))
      true
      (catch Exception e
        (reset! banco {})
        (reset! usuario-ativo nil)
        {:error (str "Erro crítico ao carregar dados de " arquivo ": " (.getMessage e))})))
    {:error (str "Arquivo de dados não encontrado: " arquivo)})

(when (empty? @banco)
  (carregar))

(defn registrar-usuario [dados-input]
  (let [username (:username dados-input)
        id username]
    (if (str/blank? username)
      {:error "Username cannot be blank."}
      (if (contains? @banco id)
        (let [existing-user-data (get-in @banco [id :dados])]
          (reset! usuario-ativo existing-user-data)
          (salvar)
          {:user @usuario-ativo :status :loaded-existing})
        (do
          (swap! banco assoc id {:dados (assoc dados-input :id id) :transacoes '()})
          (reset! usuario-ativo (get-in @banco [id :dados]))
          (salvar)
          {:user @usuario-ativo :status :new-user-registered})))))

(defn obter-usuario []
  @usuario-ativo)

(defn listar-usuarios []
  (mapv (fn [[_ user-entry]] (:dados user-entry)) @banco))

(defn registrar [transacao]
  (if-let [active-user-id (:id @usuario-ativo)]
    (do
      (swap! banco update-in [active-user-id :transacoes] conj transacao)
      (salvar)
      (merge transacao {:id (count (get-in @banco [active-user-id :transacoes]))}))
    {:error "No active user to register transaction."}))

(defn limpar []
  (if-let [active-user-id (:id @usuario-ativo)]
    (do
      (swap! banco assoc-in [active-user-id :transacoes] '())
      (salvar)
      true)
    {:error "No active user to clear history."}))

(defn- exercicio? [transacao]
  (= (:tipo transacao) "exercicio"))

(defn transacoes []
  (if-let [active-user-id (:id @usuario-ativo)]
    (get-in @banco [active-user-id :transacoes])
    '()))

(defn saldo []
  (let [ts (transacoes)
        consumidas (reduce + 0 (map :valor (remove exercicio? ts)))
        gastas (reduce + 0 (map :valor (filter exercicio? ts)))
        saldo (- consumidas gastas)]
    {:consumidas consumidas :gastas gastas :saldo saldo}))

(defn transacoes-por-data-global [data]
  (for [[id {:keys [dados transacoes]}] @banco
        :let [filtradas (filter #(= (:data %) data) transacoes)]
        :when (seq filtradas)]
    {:usuario dados
     :transacoes filtradas}))

(defn saldo-por-data-global [data]
  (map (fn [{:keys [usuario transacoes]}]
         (let [consumidas (reduce + 0 (map :valor (remove exercicio? transacoes)))
               gastas (reduce + 0 (map :valor (filter exercicio? transacoes)))
               saldo (- consumidas gastas)]
           {:usuario usuario
            :data data
            :consumidas consumidas
            :gastas gastas
            :saldo saldo}))
       (transacoes-por-data-global data)))

(defn saldo-por-usuario [id]
  (let [user-entry (get @banco id)
        ts (:transacoes user-entry)
        consumidas (reduce + 0 (map :valor (remove exercicio? ts)))
        gastas (reduce + 0 (map :valor (filter exercicio? ts)))
        saldo (- consumidas gastas)
        dados (:dados user-entry)]
    {:usuario dados
     :consumidas consumidas
     :gastas gastas
     :saldo saldo}))

(defn saldo-total-global []
  (map (fn [[id _]] (saldo-por-usuario id)) @banco))

(defn parse-data [s]
  (LocalDate/parse s fmt))

(defn transacoes-entre-datas [transacoes data-inicio data-fim]
  (let [di (parse-data data-inicio)
        df (parse-data data-fim)]
    (seq
      (filter (fn [t]
                (let [d (parse-data (:data t))]
                  (and (not (.isBefore d di))
                       (not (.isAfter d df)))))
              transacoes))))

(defn transacoes-por-periodo-global [data-inicio data-fim]
  (for [[id {:keys [dados transacoes]}] @banco
        :let [filtradas (transacoes-entre-datas transacoes data-inicio data-fim)]
        :when (seq filtradas)]
    {:usuario dados
     :transacoes filtradas}))

(defn saldo-por-periodo-global [data-inicio data-fim]
  (map (fn [{:keys [usuario transacoes]}]
         (let [consumidas (reduce + 0 (map :valor (remove exercicio? transacoes)))
               gastas (reduce + 0 (map :valor (filter exercicio? transacoes)))
               saldo (- consumidas gastas)]
           {:usuario usuario
            :data-inicio data-inicio
            :data-fim data-fim
            :consumidas consumidas
            :gastas gastas
            :saldo saldo}))
       (transacoes-por-periodo-global data-inicio data-fim)))

(defn transacoes-por-periodo-usuario [data-inicio data-fim]
  (if-let [active-user-id (:id @usuario-ativo)]
    (let [todas-transacoes (get-in @banco [active-user-id :transacoes])]
      (transacoes-entre-datas todas-transacoes data-inicio data-fim))
    '()))

(defn saldo-por-periodo-usuario [data-inicio data-fim]
  (if-let [active-user-id (:id @usuario-ativo)]
    (let [transacoes (transacoes-por-periodo-usuario data-inicio data-fim)
          consumidas (reduce + 0 (map :valor (remove exercicio? transacoes)))
          gastas (reduce + 0 (map :valor (filter exercicio? transacoes)))
          saldo (- consumidas gastas)]
      {:data-inicio data-inicio
       :data-fim data-fim
       :consumidas consumidas
       :gastas gastas
       :saldo saldo})
    {:consumidas 0 :gastas 0 :saldo 0 :error "No active user to get period balance."}))