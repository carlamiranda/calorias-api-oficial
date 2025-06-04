(ns calorias-front.core
   (:gen-class)
   (:require [clj-http.client :as http]
             [cheshire.core :as json]
             [clojure.string :as str]))



 (def api-url "http://localhost:3000")

 (defn buscar-alimento [nome]
   (:body (http/get (str api-url "/alimento")
                    {:query-params {"nome" nome} :as :json})))

 (defn buscar-exercicio [nome peso tempo altura idade genero]
   (:body (http/get (str api-url "/exercicio")
                    {:query-params {"nome" nome
                                    "peso" (str peso)
                                    "tempo" (str tempo)
                                    "altura" (str altura)
                                    "idade" (str idade)
                                    "genero" genero}
                     :as :json})))

 (defn buscar-saldo []
   (-> (http/get (str api-url "/saldo") {:as :json}) :body :saldo))

 (defn buscar-transacoes []
   (-> (http/get (str api-url "/transacoes") {:as :json}) :body))

 (defn buscar-saldo-data-global [data]
   (-> (http/get (str api-url "/saldo/data/global")
                 {:query-params {"data" data}
                  :as :json})
       :body))

 (defn buscar-transacoes-data-global [data]
   (-> (http/get (str api-url "/transacoes/data/global")
                 {:query-params {"data" data}
                  :as :json})
       :body))

 (defn buscar-saldo-total-global []
   (-> (http/get (str api-url "/saldo/global") {:as :json}) :body))

 (defn buscar-todos-usuarios []
   (-> (http/get (str api-url "/usuarios") {:as :json}) :body))

 (defn limpar-transacoes []
   (http/post (str api-url "/limpar"))
   (println "Histórico apagado com sucesso."))

 (defn registrar-usuario [username altura peso idade genero]
  (http/post (str api-url "/usuario")
             {:body (json/generate-string {:username username
                                          :altura altura
                                          :peso peso
                                          :idade idade
                                          :genero genero})
              :headers {"Content-Type" "application/json"}
              :as :json}))


 (defn buscar-usuario []
   (:body (http/get (str api-url "/usuario") {:as :json})))

 (defn apresentar-usuario [usuario]
  (str "Usuário: " (:username usuario) ", "
       "Altura: " (:altura usuario) "cm, "
       "Peso: " (:peso usuario) "kg, "
       "Idade: " (:idade usuario) ", "
       "Gênero: " (:genero usuario)))


 (defn mostrar-usuario []
   (let [usuario (buscar-usuario)]
     (println (apresentar-usuario usuario))))


 (defn registrar-exercicio [entrada peso altura idade genero]
   (let [[exercicio tempo-str] (str/split entrada #" ")
         tempo (Integer/parseInt (str/replace tempo-str "min" ""))]
     (try
       (let [gasto (buscar-exercicio exercicio peso tempo altura idade genero)]
         (str "Gastou " (int (:valor gasto)) " cal com " (:nome gasto)))
       (catch Exception _
         "Exercício não reconhecido."))))

 (defn registrar-alimento [entrada]
   (try
     (let [cal (buscar-alimento entrada)]
       (str "Ingeriu " (int (:valor cal)) " cal com " (:nome cal)))
     (catch Exception _
       "Alimento não reconhecido.")))

 (defn registrar [entrada peso altura idade genero]
   (if (str/includes? entrada "min")
     (registrar-exercicio entrada peso altura idade genero)
     (registrar-alimento entrada)))

(defn buscar-saldo-global-intervalo [data-inicio data-fim]
  (-> (http/get (str api-url "/saldo/periodo/global")
               {:query-params {"data-inicio" data-inicio
                               "data-fim" data-fim}
                :as :json})
      :body))

(defn buscar-transacoes-global-intervalo [data-inicio data-fim]
  (-> (http/get (str api-url "/transacoes/periodo/global")
               {:query-params {"data-inicio" data-inicio
                               "data-fim" data-fim}
                :as :json})
      :body))

 (defn menu []
   (println "\n--- Menu ---")
   (println "1 - Registrar alimento ou exercício")
   (println "2 - Ver saldo total de calorias (usuário atual)")
   (println "3 - Ver histórico de transações (usuário atual)")
   (println "4 - Limpar histórico (usuário atual)")
   (println "5 - Mostrar usuário atual")
   (println "6 - Listar todos os usuários registrados")
   (println "7 - Consultar saldo global por período (todos os usuários)")
   (println "8 - Consultar transações globais por período (todos os usuários)")
   (println "9 - Consultar saldo total global (todos os usuários)")
   (println "10 - Sair do sistema"))

 (defn opcoes-menu [username peso altura idade genero]
   (menu)
   (print "Escolha uma opção: ") (flush)
   (let [opcao (read-line)]
     (case opcao
       "1" (do
             (println "Digite alimento ou exercício (ex: banana / running 30min). Digite 'finalizar' para encerrar:")
             (doall (map println
                         (map #(registrar % peso altura idade genero)
                              (take-while #(not (#{"finalizar" "Finalizar"} %))
                                          (repeatedly #(read-line))))))
         
             (recur username peso altura idade genero))
 
       "2" (do
             (let [dados (buscar-saldo)]
               (println "\nSaldo total atual:")
               (println (str "Consumidas: " (:consumidas dados)))
               (println (str "Gastas: " (:gastas dados)))
               (println (str "Saldo: " (:saldo dados))))
             (recur username peso altura idade genero))
 
       "3" (do
             (doall (map println (buscar-transacoes)))
             (recur username peso altura idade genero))
 
       "4" (do
             (limpar-transacoes)
             (recur username peso altura idade genero))
 
       "5" (do
             (mostrar-usuario)
             (recur username peso altura idade genero))
 
       "6" (do
             (let [usuarios (buscar-todos-usuarios)]
               (println "\nUsuários registrados:")
               (doall (map #(println (str "\nUsuário: " (:username %)
                                          " |Altura: " (:altura %)
                                          " | Peso: " (:peso %)
                                          " | Idade: " (:idade %)
                                          " | Gênero: " (:genero %)))
                           usuarios)))
             (recur username peso altura idade genero))
 
      "7" (do
      (println "Digite a data inicial (yyyy-MM-dd):")
      (let [data-inicio (read-line)]
        (println "Digite a data final (yyyy-MM-dd):")
        (let [data-fim (read-line)
              resultados (buscar-saldo-global-intervalo data-inicio data-fim)]
          (doall
            (map (fn [r]
                   (let [usuario (:usuario r)
                         nome (:username usuario)]
                     (println (str "\nUsuário: " nome
                                   "\nConsumidas: " (:consumidas r)
                                   "\nGastas: " (:gastas r)
                                   "\nSaldo: " (:saldo r)))))
                 resultados))))
      (recur username peso altura idade genero))

      "8" (do
            (println "Digite a data inicial (yyyy-MM-dd):")
            (let [data-inicio (read-line)]
              (println "Digite a data final (yyyy-MM-dd):")
              (let [data-fim (read-line)
                    resultados (buscar-transacoes-global-intervalo data-inicio data-fim)]
                (doall (map #(println (str "\nUsuário: " (:usuario %)
                                          "\nTransações: " (:transacoes %)))
                            resultados))))
            (recur username peso altura idade genero))
 
       "9" (do
             (let [resultados (buscar-saldo-total-global)]
               (doall (map #(println (str "\nUsuário: " (:usuario %)
                                          "\nConsumidas: " (:consumidas %)
                                          " | Gastas: " (:gastas %)
                                          " | Saldo: " (:saldo %)))
                           resultados)))
             (recur username peso altura idade genero))
 
       "10" (do
              (println "Encerrando.")
              (System/exit 0))
 
       (do
         (println "Opção inválida.")
         (recur username peso altura idade genero)))))

(defn -main [& args]
  (println "Bem-vindo(a) ao Rastreador de Calorias")

  (println "Digite o seu nome de usuário:")
  (let [username (read-line)
        _ (println "Digite o seu peso (kg):")
        peso (Double/parseDouble (read-line))
        _ (println "Digite a sua altura (cm):")
        altura (Integer/parseInt (read-line))
        _ (println "Digite a sua idade:")
        idade (Integer/parseInt (read-line))
        _ (println "Digite o seu gênero (male/female):")
        genero (read-line)]
    
    (registrar-usuario username altura peso idade genero)
    (mostrar-usuario)
    (opcoes-menu username peso altura idade genero)))