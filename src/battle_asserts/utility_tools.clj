(ns battle-asserts.utility-tools
  (:require [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.tools.namespace.find :as nsf]))

(defn- collect-namespaces []
  (-> "src/battle_asserts/issues"
      io/as-file
      nsf/find-namespaces-in-dir))

(defn- prepare-namespace-name [namespace]
  (s/replace (last (s/split (str namespace) #"\.")) #"-" "_"))

(defn- present-untraslated-tasks [task-list level]
  (if (empty? task-list)
    (println (str "Tasks with level " level " translated!"))
    (println (str (s/capitalize level) " tasks without translations: \n" (s/join "\n" task-list) "\n"))))

(defn check-translations
  "Tool that check translation progress of tasks."
  [& _args]
  (let [namespaces (collect-namespaces)
        translated-count (atom 0)
        untranslated-list (atom {:elementary '()
                                 :easy '()
                                 :medium '()
                                 :hard '()})
        namespaces-count (count namespaces)]
    (doseq [namespace namespaces]
      (require namespace)
      (let [issue-name (prepare-namespace-name namespace)
            description @(ns-resolve namespace 'description)
            level @(ns-resolve namespace 'level)]
        (if (map? description)
          (swap! translated-count inc)
          (swap! untranslated-list (fn [acc el]
                                     (update-in acc [level] conj el)) issue-name))))
    (if (empty? @untranslated-list)
      (println "All tasks are translated!")
      (do
        (present-untraslated-tasks (@untranslated-list :elementary) "elementary")
        (present-untraslated-tasks (@untranslated-list :easy) "easy")
        (present-untraslated-tasks (@untranslated-list :medium) "medium")
        (present-untraslated-tasks (@untranslated-list :hard) "hard")))
    (println (str "Total translation progress " @translated-count " / " namespaces-count " or " (format "%.1f" (float (* (/ @translated-count namespaces-count) 100))) "%"))))

(defn check-tags
  "Tool that checks tags presence in tasks."
  [& _args]
  (let [namespaces (collect-namespaces)
        tagged-count (atom 0)
        untagged-list (atom (list))
        namespaces-count (count namespaces)]
    (doseq [namespace namespaces]
      (require namespace)
      (let [issue-name (prepare-namespace-name namespace)
            tags (ns-resolve namespace 'tags)
            level @(ns-resolve namespace 'level)]
        (if-not (nil? tags)
          (swap! tagged-count inc)
          (swap! untagged-list conj (str issue-name " " level)))))
    (if (empty? @untagged-list)
      (println "All tasks are tagged!")
      (println (str "Tasks without tags: \n" (s/join "\n" @untagged-list))))
    (println (str "Total tagged tasks progress " @tagged-count " / " namespaces-count " or " (format "%.1f" (float (* (/ @tagged-count namespaces-count) 100))) "%"))))

(defn check-generators-and-solutions
  "Tool that check presence of solution and arguments generator in tasks."
  [& _args]
  (let [namespaces (collect-namespaces)
        completed-task-count (atom 0)
        without-solution (atom (list))
        without-generator (atom (list))
        namespaces-count (count namespaces)]
    (doseq [namespace namespaces]
      (require namespace)
      (let [issue-name (prepare-namespace-name namespace)
            solution (ns-resolve namespace 'solution)
            generator (ns-resolve namespace 'arguments-generator)
            level @(ns-resolve namespace 'level)]
        (cond
          (and (not (nil? solution)) (not (nil? generator))) (swap! completed-task-count inc)
          (and (nil? solution) (nil? generator))
          (do
            (swap! without-solution conj (str issue-name " " level))
            (swap! without-generator conj (str issue-name " " level)))
          (nil? solution) (swap! without-solution conj (str issue-name " " level))
          (nil? generator) (swap! without-generator conj (str issue-name " " level)))))
    (if (empty? @without-solution)
      (println "All tasks have solution!")
      (println (str "Tasks without solution: \n" (s/join "\n" @without-solution))))
    (if (empty? @without-generator)
      (println "All tasks have arguments generator!")
      (println (str "Tasks without arguments generator: \n" (s/join "\n" @without-generator))))
    (println (str "Total tasks solution and generator progress " @completed-task-count " / " namespaces-count " or " (format "%.1f" (float (* (/ @completed-task-count namespaces-count) 100))) "%"))))

(defn collect-tags
  "Tool that collect tags stats of tasks."
  [& _args]
  (let [namespaces (collect-namespaces)
        tags-list (atom [])]
    (doseq [namespace namespaces]
      (require namespace)
      (let [issue-name (prepare-namespace-name namespace)
            tags (ns-resolve namespace 'tags)]
        (if-not (nil? tags)
          (swap! tags-list (fn [acc elem] (apply conj acc elem)) @tags)
          (println (str "Task " issue-name " without tags!")))))
    (let [stats (sort-by first (frequencies @tags-list))
          incorrect-tag-number 1
          probably-incorrect (mapv first (filterv
                                          #(= (second %) incorrect-tag-number) stats))]
      (println "Total tags stats:" (s/join ", " (mapv #(s/join ": " %) stats)))
      (println "Probably misspelled tags:" (s/join ", " probably-incorrect)))))

(defn collect-disabled
  "Tool that collects all disabled tasks."
  [& _args]
  (let [namespaces (collect-namespaces)
        disabled-list (atom [])]
    (doseq [namespace namespaces]
      (require namespace)
      (let [issue-name (prepare-namespace-name namespace)
            disabled (ns-resolve namespace 'disabled)]
        (when (and (not (nil? disabled)) @disabled)
          (swap! disabled-list conj issue-name))))
    (if (empty? @disabled-list)
      (println "There is no disabled tasks! Yaay!")
      (println (str "Disabled tasks list:\n" (s/join ", " @disabled-list) "\nTotal number of disabled tasks is: " (count @disabled-list) "!")))))

(defn- generate-isssue-template [issue-name]
  (let [issue-filename (s/replace issue-name #" " "_")
        issue-file (io/as-file (str "./src/battle_asserts/issues/" issue-filename ".clj"))
        issue-body-template "  (:require [clojure.test.check.generators :as gen]))
        
; FIXME add actual task level (required)
(def level :elementary)

; FIXME add actual tags (required)
(def tags [])

(def description {:en \"Add issue description in english (required)\" :ru \"Add issue description in russian (optional)\"})

; FIXME add actual signature (required)
(def signature {:input [{}] :output {}})

; FIXME add correct arguments-generator for samples (optional, you can delete args-generator)
(defn arguments-generator [] (gen/tuple))

; FIXME add actual data (required)
(def test-data [{:expected [] :arguments []}])

; FIXME add actual solution for issue (optional, you can delete solution)
(defn solution [args])
"
        issue-ns (s/replace issue-name #" " "-")
        issue-template (str "(ns battle-asserts.issues." issue-ns "\n" issue-body-template)]
    (if (.exists issue-file)
      (println "Issue template already exist!")
      (do
        (spit issue-file issue-template)
        (println (str "Issue template generated, checkout file " (.getAbsolutePath issue-file)))))))

(defn run-isssue-template-generator
  "Tool that generates issue template with hints"
  [& _args]
  (println "Welcome to issue template generator! May I have issue name? (for exit just press ENTER)")
  (let [issue-name (s/trim (read-line))]
    (if (= issue-name "")
      (println "Bye!")
      (generate-isssue-template (s/lower-case issue-name)))))