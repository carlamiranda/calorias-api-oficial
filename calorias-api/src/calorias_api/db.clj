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

(defn data-hoje []
  (.format (LocalDate/now) fmt))

(defn gerar-id-usuario [dados]
  (str (:altura dados) "-" (:peso dados) "-" (:idade dados) "-" (:genero dados)))

(defn salvar []
  (spit arquivo (json/generate-string {:banco @banco :usuario @usuario-ativo})))

(defn carregar []
  (when (.exists (io/file arquivo))
    (let [dados (json/parse-string (slurp arquivo) true)]
      (reset! banco (:banco dados))
      (reset! usuario-ativo (:usuario dados)))))

(carregar)

(defn registrar-usuario [dados]
  (let [username (:username dados)]
    (if (and username (not (str/blank? username)))
      (do
        (reset! usuario-ativo (assoc dados :id username))
        (when-not (contains? @banco username)
          (swap! banco assoc username {:dados (assoc dados :id username)
                                      :transacoes '()}))
        (salvar)))))


(defn obter-usuario []
  @usuario-ativo)

(defn listar-usuarios []
  (mapv (fn [[id {:keys [dados]}]] dados) @banco))

(defn registrar [transacao]
  (let [id (:id @usuario-ativo)
        transacao-com-data (assoc transacao :data (data-hoje))]
    (swap! banco update-in [id :transacoes] conj transacao-com-data)
    (salvar)
    (merge transacao-com-data {:id (count (get-in @banco [id :transacoes]))})))

(defn limpar []
  (let [id (:id @usuario-ativo)]
    (swap! banco assoc-in [id :transacoes] '())
    (salvar)))

(defn- exercicio? [transacao]
  (= (:tipo transacao) "exercicio"))

(defn transacoes []
  (get-in @banco [(:id @usuario-ativo) :transacoes]))

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
  (let [ts (get-in @banco [id :transacoes])
        consumidas (reduce + 0 (map :valor (remove exercicio? ts)))
        gastas (reduce + 0 (map :valor (filter exercicio? ts)))
        saldo (- consumidas gastas)
        dados (get-in @banco [id :dados])]
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
