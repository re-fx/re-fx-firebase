(ns re-fx.firebase
  (:require [reagent.core :as r]
            [re-frame.core :as re]
            [clojure.string :as st])
  (:import [goog]))

(goog-define api-key "")
(goog-define auth-domain "")
(goog-define database-url "")
(goog-define storage-bucket "")
(assert (not (= "" api-key)))
(assert (not (= "" auth-domain)))
(assert (not (= "" database-url)))
(assert (not (= "" storage-bucket)))

;; stolen from lein-figwheel
(defn react-native-env? [] (and (exists? goog/global.navigator)
                                (= goog/global.navigator.product "ReactNative")))

(defonce firebase (if (react-native-env?)
                      (js/require "firebase/firebase-react-native")
                      (js/require "firebase")))

(defonce root (.initializeApp firebase
                              (clj->js {:apiKey        api-key
                                        :authDomain    auth-domain
                                        :databaseURL   database-url
                                        :storageBucket storage-bucket})))

(assert firebase)
(assert root)

(re/reg-fx
 :firebase-login
 (fn firebase-login [{:keys [username password on-success on-failure]
                      :or   {on-success [:firebase-login-no-on-success]
                             on-failure [:firebase-login-no-on-failure]}}]
   (.then (.signInWithEmailAndPassword (.auth root) username password)
          (fn firebase-login-on-success [] (re/dispatch on-success))
          (fn firebase-login-on-failure [] (re/dispatch on-failure)))))

(re/reg-fx
 :firebase-login-with-custom-token
 (fn firebase-login-with-custom-token [{:keys [token on-success on-failure]
                                        :or   {on-success [:firebase-login-with-custom-token-no-on-success]
                                               on-failure [:firebase-login-with-custom-token-no-on-failure]}}]
   (.then (.signInWithCustomToken (.auth root) token)
          (fn firebase-login-with-custom-token-on-success [] (re/dispatch on-success))
          (fn firebase-login-with-custom-token-on-failure [] (re/dispatch on-failure)))))

(re/reg-fx
 :firebase-logout
 (fn firebase-logout [{:keys [on-success on-failure]
                       :or   {on-success [:firebase-logout-no-on-success]
                              on-failure [:firebase-logout-no-on-failure]}}]
   (.then (.signOut (.auth root))
          (fn firebase-logout-on-success [] (re/dispatch on-success))
          (fn firebase-logout-on-failure [] (re/dispatch on-failure)))))

(defn pretty-key [s]
  (if (not (nil? (re-matches #"^-?[0-9]+" s)))
    (int s)
    (if (string? s)
      (keyword s)
      s)))

(re/reg-fx
 :firebase-listen-value
 (fn firebase-listen [{:keys [path value-changed]
                       :or {path ""
                            value-changed [(keyword (str "firebase-listen-value-m-no-value-changed"))]}}]
   (let [ref (.ref (.database root) path)]
     (.on ref "value" (fn firebase-listen-value-changed [c]
                        (let [vv (.val c)]
                          (re/dispatch (conj value-changed vv))))))))

(re/reg-fx
 :firebase-listen
 (fn firebase-listen [ms]
   (->> ms
        (map (fn firebase-listen-item [[m v]]
               (let [{:keys [child-added child-changed child-removed path]
                      :or   {child-added   [(keyword (str "firebase-listen-" m "-no-child-added"))]
                             child-changed [(keyword (str "firebase-listen-" m "-no-child-changed"))]
                             child-removed [(keyword (str "firebase-listen-" m "-no-child-removed"))]}} v
                     ref (.ref (.database root) path)
                     mk (keyword m)]
                 (doto ref
                   (.on "child_added" (fn firebase-listen-on-child-added [c]
                                        (let [k (.-key c)
                                              v (.val c)
                                              kk (pretty-key k)
                                              vv (js->clj v :keywordize-keys true)]
                                          (if (not (= "dummy" k))
                                            (re/dispatch (conj child-added mk kk vv v))))))
                   (.on "child_changed" (fn firebase-listen-on-child-changed [c]
                                          (let [k (.-key c)
                                                v (.val c)
                                                kk (pretty-key k)
                                                vv (js->clj v :keywordize-keys true)]
                                            (if (not (= "dummy" k))
                                              (re/dispatch (conj child-changed mk kk vv v))))))
                   (.on "child_removed" (fn firebase-listen-on-child-removed [c]
                                          (let [k (.-key c)
                                                kk (pretty-key k)]
                                            (if (not (= "dummy" k))
                                              (re/dispatch (conj child-removed mk kk))))))))))
        (doall))))

(re/reg-fx
 :firebase-update
 (fn firebase-update [{:keys [path value on-success on-failure]
                       :or   {on-success [:firebase-update-no-on-success]
                              on-failure [:firebase-update-no-on-failure]}}]
   (.then (.update (.ref (.database root) path) (clj->js value))
          (fn firebase-update-on-success []
            (re/dispatch on-success))
          (fn firebase-update-on-failure [e]
            (re/dispatch on-failure)))))

(re/reg-fx
 :firebase-remove
 (fn firebase-remove [{:keys [path on-success on-failure]
                       :or   {on-success [:firebase-remove-no-on-success]
                              on-failure [:firebase-remove-no-on-failure]}}]
   (.then (.remove (.ref (.database root) path))
          (fn firebase-remove-on-success []
            (re/dispatch on-success))
          (fn firebase-remove-on-failure []
            (re/dispatch on-failure)))))

(re/reg-fx
 :firebase-push
 (fn firebase-push [{:keys [path value on-success on-failure]
                     :or   {on-success [:firebase-push-no-on-success]
                            on-failure [:firebase-push-no-on-failure]
                            value      {}}}]
   (.then (.push (.ref (.database root) path) (clj->js value))
          (fn firebase-push-on-success [snapshot] (re/dispatch (conj on-success (.-key snapshot))))
          (fn firebase-push-on-failure [] (re/dispatch on-failure)))))

(re/reg-fx
 :firebase-auth-state-changed
 (fn firebase-auth-state-changed [{:keys [on-signed-in on-signed-out]
                                   :or   {on-signed-in  [:firebase-auth-state-changed-no-on-signed-in]
                                          on-signed-out [:firebase-auth-state-changed-no-on-signed-out]}}]
   (.onAuthStateChanged
    (.auth root)
    (fn firebase-auth-state-changed-auth-state-changed [user]
      (re/dispatch (if user
                     (conj on-signed-in user)
                     on-signed-out))))))
