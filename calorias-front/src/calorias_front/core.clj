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

 (defn registrar-usuario [altura peso idade genero]
   (http/post (str api-url "/usuario")
              {:body (json/generate-string {:altura altura :peso peso :idade idade :genero genero})
               :headers {"Content-Type" "application/json"}
               :as :json}))

 (defn buscar-usuario []
   (:body (http/get (str api-url "/usuario") {:as :json})))

 (defn apresentar-usuario [usuario]
   (str "\nUsuário registrado: "
        "Altura: " (:altura usuario) "cm, "
        "Peso: " (:peso usuario) "kg, "
        "Idade: " (:idade usuario) ", "
        "Gênero: " (:genero usuario)))

 (defn mostrar-usuario []
   (let [usuario (buscar-usuario)]
     (println (apresentar-usuario usuario))))

 ;; ====== Registro de transações ======

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

 ;; ====== Menu principal ======

 (defn menu []
   (println "\n--- Menu ---")
   (println "1 - Adicionar alimento ou exercício")
   (println "2 - Ver saldo total")
   (println "3 - Ver histórico de transações")
   (println "4 - Limpar histórico")
   (println "5 - Mostrar usuário atual")
   (println "6 - Listar todos os usuários registrados")
   (println "7 - Consultar saldo global por data (todos usuários)")
   (println "8 - Consultar transações globais por data")
   (println "9 - Consultar saldo total global (todos usuários)")
   (println "10 - Sair"))

 (defn opcoes-menu [peso altura idade genero]
   (menu)
   (print "Escolha uma opção: ") (flush)
   (let [opcao (read-line)]
     (case opcao
       "1" (do
             (println "Digite alimento ou exercício (ex: banana / running 30min). Digite 'finalizar' para encerrar:")
             (doall (map println
                         (map #(registrar % peso altura idade genero)
                              (take-while #(not (#{"finalizar" "Finalizar"} %))
                                          (repeatedly #(read-line)))))))

       "2" (let [dados (buscar-saldo)]
             (println "\nSaldo total atual:")
             (println (str "Consumidas: " (:consumidas dados)))
             (println (str "Gastas: " (:gastas dados)))
             (println (str "Saldo: " (:saldo dados))))

     "3" (doall (map println (buscar-transacoes)))

     "4" (limpar-transacoes)

     "5" (mostrar-usuario)

     "6" (let [usuarios (buscar-todos-usuarios)]
           (println "\nUsuários registrados:")
           (doall (map #(println (str "\nAltura: " (:altura %)
                                      " | Peso: " (:peso %)
                                      " | Idade: " (:idade %)
                                      " | Gênero: " (:genero %)))
                       usuarios)))

     "7" (let [data (do (println "Digite a data (yyyy-MM-dd):") (read-line))
               resultados (buscar-saldo-data-global data)]
           (doall (map #(println (str "\nUsuário: " (:usuario %)
                                      "\nConsumidas: " (:consumidas %)
                                      "\nGastas: " (:gastas %)
                                      "\nSaldo: " (:saldo %)))
                       resultados)))

     "8" (let [data (do (println "Digite a data (yyyy-MM-dd):") (read-line))
               resultados (buscar-transacoes-data-global data)]
           (doall (map #(println (str "\nUsuário: " (:usuario %)
                                      "\nTransações: " (:transacoes %)))
                       resultados)))

     "9" (let [resultados (buscar-saldo-total-global)]
           (doall (map #(println (str "\nUsuário: " (:usuario %)
                                      "\nConsumidas: " (:consumidas %)
                                      " | Gastas: " (:gastas %)
                                      " | Saldo: " (:saldo %)))
                       resultados)))

     "10" (do (println "Encerrando.") (System/exit 0))

     (println "Opção inválida."))))
;; ====== Entrada principal ======

(defn -main [& args]
  (println "Bem-vindo(a) ao Rastreador de Calorias")

  (println "Digite seu peso (kg):")
  (let [peso (Double/parseDouble (read-line))
        _ (println "Digite sua altura (cm):")
        altura (Integer/parseInt (read-line))
        _ (println "Digite sua idade:")
        idade (Integer/parseInt (read-line))
        _ (println "Digite seu gênero (male/female):")
        genero (read-line)]

    (registrar-usuario altura peso idade genero)
    (mostrar-usuario)
    (opcoes-menu peso altura idade genero)))