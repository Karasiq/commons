package com.karasiq.common

package object factory {
  type Factory[R] = () ⇒ R
  type ParametrizedFactory[P, R] = P ⇒ R
}
