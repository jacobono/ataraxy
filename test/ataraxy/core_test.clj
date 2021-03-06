(ns ataraxy.core-test
  (:require [clojure.test :refer :all]
            [ataraxy.core :as ataraxy]))

(deftest test-valid?
  (are [x y] (= (ataraxy/valid? x) y)
    '{"/foo" [:bar]} true
    '{"/foo" :bar}   false
    '{"/foo" (:bar)} false))

(deftest test-matches
  (testing "string routes"
    (let [routes '{"/foo" [:foo], "/bar" [:bar]}]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:uri "/foo"}       [:foo]
        {:path-info "/bar"} [:bar]
        {:uri "/baz"}       nil)))

  (testing "list routing tables"
    (let [routes '("/foo" [:foo], "/bar" [:bar])]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:uri "/foo"}       [:foo]
        {:path-info "/bar"} [:bar]
        {:uri "/baz"}       nil)))

  (testing "symbol routes"
    (let [routes '{x [:foo x]}]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:uri "foo"}       [:foo "foo"]
        {:path-info "bar"} [:foo "bar"])))

  (testing "custom regexes"
    (let [routes '{^{:re #"\d\d"} x   [:foo x]
                   ^{:re #"\d\d\d"} y [:bar y]}]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:uri "10"}     [:foo "10"]
        {:uri "1"}      nil
        {:uri "bar"}    nil
        {:uri "10/bar"} nil
        {:uri "200"}    [:bar "200"]
        {:uri "1"}      nil)))

  (testing "keyword routes"
    (let [routes '{:get [:read], :put [:write]}]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:request-method :get}    [:read]
        {:request-method :put}    [:write]
        {:request-method :delete} nil)))

  (testing "set routes"
    (let [routes '{#{x} [:foo x], #{y} [:bar y], #{z w} [:baz z w]}]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:uri "/" :query-params {"x" "quz"}}       [:foo "quz"]
        {:uri "/" :query-params {"y" "quz"}}       [:bar "quz"]
        {:uri "/" :query-params {"z" "a" "w" "b"}} [:baz "a" "b"]
        {:uri "/" :form-params {"x" "fp"}}         [:foo "fp"]
        {:uri "/" :multipart-params {"x" "mp"}}    [:foo "mp"]
        {:uri "/" :query-params {"z" "a"}}         nil
        {:uri "/"}                                 nil)))

  (testing "map routes"
    (let [routes '{{{p :p} :params} [:p p], {{:keys [q]} :params} [:q q]}]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:params {:p "page"}}    [:p "page"]
        {:params {:q "query"}}   [:q "query"]
        {:params {:z "invalid"}} nil)))

  (testing "compiled routes"
    (let [routes (ataraxy/compile '{"/foo" [:foo]})]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:uri "/foo"} [:foo]
        {:uri "/bar"} nil)))

  (testing "vector routes"
    (let [routes '{["/foo/" foo]          [:foo foo]
                   [:get "/bar"]          [:bar]
                   ["/baz" #{baz}]        [:baz baz]
                   [:get "/x/" x "/y/" y] [:xy x y]}]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:uri "/foo/10"}                         [:foo "10"]
        {:request-method :get, :uri "/bar"}      [:bar]
        {:uri "/baz", :query-params {"baz" "2"}} [:baz "2"]
        {:request-method :get, :uri "/x/8/y/3a"} [:xy "8" "3a"]
        {:uri "/foo"}                            nil
        {:request-method :put, :uri "/bar"}      nil
        {:request-method :get, :uri "/x/44/y/"}  nil)))

  (testing "nested routes"
    (let [routes '{"/foo" {:get [:foo], ["/" id] {:get [:foo id]}}}]
      (are [req res] (= (ataraxy/matches routes req) res)
        {:request-method :get, :uri "/foo"}    [:foo]
        {:request-method :get, :uri "/foo/10"} [:foo "10"]
        {:uri "/foo"}                          nil))))
