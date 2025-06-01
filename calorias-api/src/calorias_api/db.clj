(ns calorias-api.db
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io])
  (:import [java.time LocalDate]
           [java.time.format DateTimeFormatter]))

(def arquivo "dados.json")

(def banco (atom {}))
(def usuario-ativo (atom nil))

(defn data-hoje []
  (.format (LocalDate/now) (DateTimeFormatter/ofPattern "yyyy-MM-dd")))

(defn gerar-id-usuario [dados]
  (str (:altura dados) "-" (:peso dados) "-" (:idade dados) "-" (:genero dados)))

(defn salvar []
  (spit arquivo (json/write-str {:banco @banco :usuario @usuario-ativo})))

(defn carregar []
  (when (.exists (io/file arquivo))
    (let [dados (json/read-str (slurp arquivo) :key-fn keyword)]
      (reset! banco (:banco dados))
      (reset! usuario-ativo (:usuario dados)))))

(carregar)


(defn registrar-usuario [dados]
  (let [id (gerar-id-usuario dados)]
    (reset! usuario-ativo (assoc dados :id id))
    (when-not (contains? @banco id)
      (swap! banco assoc id {:dados (assoc dados :id id)
                             :transacoes []}))
    (salvar)))

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
    (swap! banco assoc-in [id :transacoes] [])
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
