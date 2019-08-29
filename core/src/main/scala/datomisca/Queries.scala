package datomisca

import datomisca.AbstractQuery
import datomisca.macros.MacroImpl

object Queries {
  implicit class QueryHelper(private val sc: StringContext) extends AnyVal {
    def query(args: Any*): AbstractQuery = macro MacroImpl.cljQueryImpl
    def rules(args: Any*): QueryRules = macro MacroImpl.cljRulesImpl
  }
}
