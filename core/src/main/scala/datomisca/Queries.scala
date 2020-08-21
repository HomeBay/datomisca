package datomisca

import datomisca.AbstractQuery
import datomisca.macros.MacroImpl


/** 
  * String interpolator definitions for Datomic queries and rules. the actual string parsing is done in a macro so that syntax errors can be reported at compile-time  
  */
object Queries {

  implicit class QueryHelper(private val sc: StringContext) extends AnyVal {
  
    def query(args: Any*): AbstractQuery = macro MacroImpl.cljQueryImpl
    def rules(args: Any*): QueryRules = macro MacroImpl.cljRulesImpl
  }
}
