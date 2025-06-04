(ns calorias-api.handler
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [calorias-api.controller :as ctrl]))

(defroutes rotas
  (GET "/" [] "API de calorias está online.")
  (GET "/alimento" [nome data] (ctrl/handle-alimento nome data))
  (GET "/exercicio" [nome peso tempo altura idade genero data] (ctrl/handle-exercicio nome peso tempo altura idade genero data))
  (GET "/transacoes/data/global" [data] (ctrl/handle-transacoes-global data))
  (GET "/saldo/data/global" [data] (ctrl/handle-saldo-data-global data))
  (GET "/saldo/global" [] (ctrl/handle-saldo-total-global))
  (GET "/transacoes" [] (ctrl/handle-transacoes))
  (GET "/saldo" [] (ctrl/handle-saldo))
  (POST "/limpar" [] (ctrl/handle-limpar))
  (POST "/usuario" request (ctrl/handle-registrar-usuario request))
  (GET "/usuarios" [] (ctrl/handle-listar-usuarios))
  (GET "/usuario" [] (ctrl/handle-obter-usuario))
  (GET "/transacoes/periodo/global" request (ctrl/handle-transacoes-intervalo request))
  (GET "/saldo/periodo/global" request (ctrl/handle-saldo-intervalo request))
  (GET "/transacoes/periodo/usuario" request (ctrl/handle-transacoes-periodo-usuario request))
  (GET "/saldo/periodo/usuario" request (ctrl/handle-saldo-periodo-usuario request))

  (route/not-found "404 - Recurso não encontrado"))

(def app
  (-> rotas
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (wrap-defaults api-defaults)))
