/*
 * Copyright 2012 Pellucid and Zenexity
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datomisca
package macros

import clojure.lang.Keyword

private[datomisca] trait ExtraMacros {
  /** Parse the string representation of a Clojure keyword into a [[Keyword]] object.
    *
    * Implemented as a macro that generates a keyword literal at compile time.
    *
    * {{{val kw = KW(":namespace/name")}}}
    *
    * @param str a Clojure keyword as a string
    * @return a Clojure [[Keyword]]
    */
  def KW(str: String): Keyword = macro MacroImpl.KWImpl
}
