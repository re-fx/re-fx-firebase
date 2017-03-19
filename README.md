# re-fx-firebase

A Clojurescript library for [firebase](https://www.npmjs.com/package/firebase)
Meant for use with re-frame

Tracking  | Artifact
----------|---------|
`3.7.2`   | `[re-fx/firebase "0.0.1-SNAPSHOT"]`

## npm

```
npm i firebase@3.7.2 --save
```

## closure-defines

```
re_fx.firebase.api_key
re_fx.firebase.auth_domain
re_fx.firebase.database_url
re_fx.firebase.storage_bucket
```

## usage

```clojure
(re/reg-event-db
  :firebase-child-added
  (fn firebase-child-added-event [db [_ m k v]]
    (update-in db [:firebase/db m] assoc k v))))

(re/reg-event-db
  :firebase-child-changed
  (fn firebase-child-changed-event [db [_ m k v]]
    (update-in db [:firebase/db m] assoc k v))))

(re/reg-event-db
  :firebase-child-removed
  (fn firebase-child-removed-event [db [_ m k]]
    (update-in db [:firebase/db m] dissoc k)))

(re/reg-event-fx
 :firebase-on-signed-in
 (fn firebase-on-signed-in-event [{:keys [db]}]
   {}))

(re/reg-event-fx
 :firebase-on-signed-out
 (fn firebase-on-signed-out-event [{:keys [db]} [_]]
   {}))

(let [p {:child-added   [:firebase-child-added]
         :child-changed [:firebase-child-changed]
         :child-removed [:firebase-child-removed]}]
  {:firebase-listen             {"reports" p
                                 "projects"  p}
   :firebase-auth-state-changed {:on-signed-in  [:firebase-on-signed-in]
                                 :on-signed-out [:firebase-on-signed-out]}})
```
