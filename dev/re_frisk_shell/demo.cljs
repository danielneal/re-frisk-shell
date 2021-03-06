(ns re-frisk-shell.demo
  (:require
    [reagent.core :as reagent]
    [taoensso.sente  :as sente]
    [taoensso.sente.packers.transit :as sente-transit]
    [re-frisk-shell.re-com.views :as ui-re-com])
  (:require-macros [reagent.ratom :refer [reaction]]))

(defonce re-frame-data (reagent/atom {:app-db (reagent/atom "not connected")
                                      :id-handler (reagent/atom "not connected")}))

(defonce re-frame-events (reagent/atom []))
(defonce deb-data (reagent/atom {}))

(defn update-app-db [val]
  (reset! (:app-db @re-frame-data) val))

(defn update-events [val]
  (swap! re-frame-events conj val))

(defn update-id-handler [val]
  (reset! (:id-handler @re-frame-data) val))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
        "/chsk" ; Must match server Ring routing URL
        {:type   :auto
         :host   (str "localhost:4567") ;js/location.port)
         :packer (sente-transit/get-transit-packer)})]
  (def ch-chsk ch-recv)) ; ChannelSocket's receive channel

;SENTE HANDLERS
(defmulti -event-msg-handler "Multimethod to handle Sente `event-msg`s" :id)

(defn event-msg-handler
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler :default [_])

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (case (first ?data)
    :refrisk/app-db (update-app-db (second ?data))
    :refrisk/events (update-events (second ?data))
    :refrisk/id-handler (update-id-handler (second ?data))))

;SENTE ROUTER
(defonce router_ (atom nil))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
            ch-chsk event-msg-handler)))

;REAGENT RENDER
(defn mount []
  (reagent/render [ui-re-com/main re-frame-data re-frame-events]
                  (.getElementById js/document "app")))

;ENTRY POINT
(defn ^:export run [port]
  (start-router!)
  (mount))

(defn on-js-reload []
  (mount))

(comment (on-js-reload) (run)) ; removing warning in IDEA