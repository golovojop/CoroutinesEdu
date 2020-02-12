package k.s.yarlykov.coroutinesedu

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.*


fun main(args: Array<String>) {

//    val future : CompletableFuture<Int>? = null
//
//    val seq = sequence {
//        for (i in 1..10) yield(i * i)
//        println("after first yield")
//
//        yield(50)
//        println("after second yield")
//
//        yieldAll(60..80 step 2)
//        println("over")
//    }
//
//    print(seq.toList())


//    callLambda {
//        print("lambda-argument inlined\n")
//        return@callLambda
//    }
//    print("After callLambda\n")


    GlobalScope.launch {
        mySuspentionProc()
    }

    Thread.sleep(1000)
    print("main was finished")

}

inline fun callLambda(block: () -> Unit) {
    print("from CallLambda\n")
    block()
}

suspend fun mySuspentionProc(): Int {

    var result = 0

    print("result before suspend: $result \n")

    result += suspendCoroutine { cont: Continuation<Int> ->
        run {
            val localResult = result + 10
            print("localResult is $localResult \n")
            Thread.sleep(500)
            cont.resumeWith(Result.success(localResult))
            cont.resume(localResult)
        }
    }

    print("result after suspend: $result \n")

    return result
}


//val testCont : Continuation<Int>? = null


/**
 * Разбор примеров кода из статьи:
 * https://github.com/Kotlin/KEEP/blob/master/proposals/coroutines.md
 */

/**
 * Смысл такой: В suspendCoroutine мы передаем алгоритм работы с Future,
 * а именно whenComplete - это функция из Future. На ней мы ожидаем
 * завершения этой Future. Возврат будет либо с результатом, либо с
 * исключением. Фактически мы определяем алгоритм работы с Future (который
 * по факту блокирующий) и оборачиваем его внутрь suspendCoroutine, задача
 * которой - предоставить Continuation, через который мы вернем результат.
 *
 * Причем
 *
 */

suspend fun <T> CompletableFuture<T>.await(): T {

    return suspendCoroutine { cont: Continuation<T> ->
        run {
            this.whenComplete { result, exception ->
                if (exception == null) // the future has been completed normally
                    cont.resume(result)
                else // the future has completed with an exception
                    cont.resumeWithException(exception)
            }
        }
    }
}

/**
 * NOTE: class Result
 *
 * Этот класс объявлен как
 * inline class Result<out T>
 * и имеет единственную переменную value: Any?
 *
 * Про inline классы: https://kotlinlang.org/docs/reference/inline-classes.html
 *
 */


/**
 * Билдер launch. ЭТОТ конкретный экземпляр работает по принципу - стартанул и забыл.
 * Это видно из типа suspend лямды: результат Unit, то есть ничего значимого.
 * В связи с этим Continuation для ДАННОЙ корутины строится таким образом, что
 * указан Result<Unit>.
 *
 * Короче ситуация начинает немного прояснятся. Когда создается корутина, то для нее
 * создается ОТДЕЛЬНЫЙ ИНСТАНС Continuation и ОТДЕЛЬНЫЙ ИНСТАНС Result. Этот Result
 * будет потом использоваться при возврате результатов из всех suspension функций
 * создаваемой корутины.
 *
 * В примере ниже мы инстанциируем Continuation для новой корутины, передавая
 * ему CoroutineContext, а также тельце функции resumeWith. Интересный нюанс -
 * внутри этой функции устанавливается handler реакции на необработанные исключения,
 * которые могу возникнуть по ходу работы. Видимо startCoroutine в процессе работы
 * создает инстансы Continuation и Result<Unit> и вызывает Continuation.resumeWith
 * с этим Result<Unit>, чтобы установить handler onFailure.
 */

fun launch(context: CoroutineContext = EmptyCoroutineContext, block: suspend () -> Unit) {

    // Инстанциируем Continuation, передавая ему CoroutineContext, а также
    // тельце функции resumeWith. Интересный нюанс - внутри этой функции
    // устанавливается handler реакции на исключение. Не могу понять в какой момент
    //
    block.startCoroutine(Continuation(context) { result : Result<Unit> ->
        //
        result.onFailure { exception ->
            val currentThread = Thread.currentThread()
            currentThread.uncaughtExceptionHandler.uncaughtException(currentThread, exception)
        }
    })

}