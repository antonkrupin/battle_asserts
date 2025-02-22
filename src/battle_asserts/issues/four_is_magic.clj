(ns battle-asserts.issues.four-is-magic
  (:require [clojure.string :as s]
            [clojure.test.check.generators :as gen]
            [clojure.pprint :as pp]))

(def level :hard)

(def tags ["strings"])

(def description
  {:en "Write a function that takes an integer number and returns an English text sequence starting with the English cardinal representation of that integer,
        the word 'is' and then the English cardinal representation of the count of characters that made up the first word, followed by a comma.
        Continue the sequence by using the previous count word as the first word of the next phrase, append 'is' and the cardinal count of the letters in that word.
        Continue until you reach four. Since four has four characters, finish by adding the words 'four is magic' and a period. All integers will eventually wind up at four. Input number is not greater than 20."
   :ru "Напишите функцию, которая берет целое число и возвращает текстовую последовательность на английском языке, начинающуюся со словесного представления исходного числа,
        далее следует слово 'is' и словесное представление количества букв получившегося первого слова, за которым следует запятая.
        Продолжите последовательность используюя предыдущее получившееся слово как первое, добавьте 'is' и словесное представление количества букв получившегося слова.
        Продолжайте пока представление количества символов не станет равным 'four'. Так как 'four' состоит из четырех символов, завершите последовательность добавив слова 'four is magic' и точку.
        Все целые числа в конечном итоге сойдутся к четырём. Гарантируется, что переданное число не больше 20."})

(def signature
  {:input [{:argument-name "num" :type {:name "integer"}}]
   :output {:type {:name "string"}}})

(defn arguments-generator []
  (gen/tuple (gen/choose 0 20)))

(def test-data
  [{:expected "Zero is four, four is magic." :arguments [0]}
   {:expected "Three is five, five is four, four is magic." :arguments [3]}
   {:expected "Four is magic." :arguments [4]}
   {:expected "Five is four, four is magic." :arguments [5]}
   {:expected "Ten is three, three is five, five is four, four is magic." :arguments [10]}
   {:expected "Fifteen is seven, seven is five, five is four, four is magic." :arguments [15]}
   {:expected "Twenty is six, six is three, three is five, five is four, four is magic." :arguments [20]}])

(defn stringify [num]
  (if (= num 4)
    "four is magic."
    (letfn [(string-num [n] (s/lower-case (pp/cl-format nil "~@(~@[~R~]~^ ~A.~)" n)))]
      (let [number (string-num num)
            size (count number)
            size-str (string-num size)]
        (str number " is " size-str ", " (stringify size))))))

(defn solution [num]
  (s/capitalize (stringify num)))
