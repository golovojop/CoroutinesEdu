package k.s.yarlykov.coroutinesedu

import kotlin.coroutines.CombinedContext
import kotlin.coroutines.CoroutineContext

interface CustomContext {

    operator fun <E : MyElement> get(key : MyKey<E>) : E?

    interface MyElement : CustomContext {
        val key: MyKey<*>
    }

    interface MyKey<E : MyElement>
}

class CustomContextList (
    private val prev: CustomContext,
    private val element: CustomContext.MyElement
) : CustomContext  {

    override fun <E : CustomContext.MyElement> get(key: CustomContext.MyKey<E>): E? {
        var cur = this
        while (true) {
            cur.element[key]?.let { return it }
            val next = cur.prev
            if (next is CustomContextList) {
                cur = next
            } else {
                return next[key]
            }
        }
    }
}

interface Element1 : CustomContext.MyElement
interface Element2 : CustomContext.MyElement
interface Element3 : CustomContext.MyElement

class CoroutineContextTest {}

fun main(args: Array<String>) {

    val e1 = object : Element1 {
        override val key = object : CustomContext.MyKey<Element1> {}
    }

    val e2 = object : Element2 {
        override val key = object : CustomContext.MyKey<Element2> {}
    }

    val e3 = object : Element3 {
        override val key = object : CustomContext.MyKey<Element3> {}
    }

    val map = hashMapOf<CustomContext.MyKey<*>, CustomContext.MyElement>(
        e1.key to e1,
        e2.key to e2,
        e3.key to e3
    )

    map.entries.forEach { entry ->
        print("Key:${entry.key.javaClass.simpleName}, Value:${entry.value.javaClass.simpleName}\n")
    }

}




