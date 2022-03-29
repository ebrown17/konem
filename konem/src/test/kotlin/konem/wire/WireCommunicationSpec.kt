package konem.wire

import io.kotest.assertions.until.fixed
import io.kotest.assertions.until.until
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.AfterTest
import io.kotest.core.spec.BeforeTest
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.datatest.withData
import konem.json.waitForMsgTime
import kotlin.time.Duration
import kotlin.time.ExperimentalTime


data class testData(val x:Int, val mutableList: MutableList<String>)


val startTest: BeforeTest = {

}

val afterTest: AfterTest = {
    println("After")
}

@ExperimentalTime
@ExperimentalKotest
class MyTests1 : ShouldSpec({
    beforeContainer { println("Before") }
    afterContainer { println("After") }

        should(": Test 2") {
            withData(
                nameFn = { t: testData -> " ${this.testCase.displayName} ${t.x} ${t.mutableList}}" },
                testData(3, arrayListOf("X","Y","Z")),
                testData(6, arrayListOf("X","Y","Z")),
                testData(8, arrayListOf("X","Y","Z")),
                testData(7, arrayListOf("X","Y","Z")),
            ) { (a, b, ) ->
                    println("TEST: $a, $b")
                    until(Duration.seconds(waitForMsgTime), Duration.milliseconds(250).fixed()) {
                        true
                    }
            }
        }
    should(": Test 3") {
        withData(
            nameFn = { t: testData -> "${this.testCase.displayName} ${t.x} ${t.mutableList}}" },
            testData(3, arrayListOf("X","Y","Z")),
            testData(6, arrayListOf("X","Y","Z")),
            testData(8, arrayListOf("X","Y","Z")),
            testData(7, arrayListOf("X","Y","Z")),
        ) { (a, b, ) ->

            println("TEST: $a, $b")
            until(Duration.seconds(waitForMsgTime), Duration.milliseconds(250).fixed()) {
                true
            }
        }
    }

})


