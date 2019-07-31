package konem.testUtil

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client
import konem.netty.stream.server.Server


class TestUtil {

    final static int MAX_CONNECT_TIME = 5000
    final static int MAX_DISCONNECT_TIME = 5000
    final static int MAX_TIME_BETWEEN_MESSAGE = 5000
    final static KonemMessageSerializer serializer = new KonemMessageSerializer()

    public static void ensureDisconnected(def clientList, max_disconnect_time = MAX_DISCONNECT_TIME) {
        def startTime = System.currentTimeMillis()
        def changedTime = startTime
        boolean allInActive = true
        while (true) {
            clientList.each { Client client ->
                if (client.isActive()) {
                    allInActive = false
                    changedTime = System.currentTimeMillis()
                }
            }

            if (allInActive) {
                Thread.sleep(100)
                break
            }
            if ((System.currentTimeMillis() - changedTime) > max_disconnect_time) {
                Thread.sleep(100)
                break
            }

            allInActive = true
            Thread.sleep(100)
        }
        def endTime = System.currentTimeMillis()

        if (allInActive) {
            println "Took ${(endTime - startTime) / 1000L} seconds for ${clientList.size()} clients to disconnect"
        } else {
            def activeCount = clientList.findAll { it.isActive() }
            println "${activeCount.size()} of ${clientList.size()} clients did not disconnect in the configured disconnect timeout of ${max_disconnect_time} seconds"
        }
    }

    public static void ensureClientsActive(def clientList, max_connect_time = MAX_CONNECT_TIME) {
        def startTime = System.currentTimeMillis()
        def changeTime = startTime
        boolean allActive = true
        while (true) {
            clientList.each { Client client ->
                if (!client.isActive()) {
                    allActive = false
                    changeTime = System.currentTimeMillis()
                }
            }

            if (allActive) {
                Thread.sleep(100)
                break
            }
            if ((System.currentTimeMillis() - startTime) > max_connect_time) {
                Thread.sleep(100)
                break
            }

            allActive = true
            Thread.sleep(100)
        }
        def endTime = System.currentTimeMillis()
        if (allActive) {
            println "Took ${(endTime - startTime) / 1000L} seconds for ${clientList.size()} clients to connect"
        } else {
            def inactiveCount = clientList.findAll { !it.isActive() }
            println "${inactiveCount.size()} of ${clientList.size()} clients did not connect in the configured connection timeout of ${max_connect_time} seconds"
        }

    }


    static KonemMessage createKonemMessage(String json) {
        return serializer.toKonemMessage(json)
    }

    public static waitForAllMessges(def readerList, int expectedNum, max_message_wait) {
        def messageCountMap = [:]
        for (Receiver reader in readerList) {
            messageCountMap.put(reader, 0)
        }
        def startTime = System.currentTimeMillis()
        def changedTime = startTime
        def noRecentMessage = true
        int received = 0
        while (true) {
            for (Receiver reader in readerList) {
                def count = messageCountMap.get(reader)
                //  println("got: $count  ")
                //  println("$received : $expectedNum")
                if (count != reader.messageCount) {
                    messageCountMap.put(reader, reader.messageCount)
                    noRecentMessage = false
                    changedTime = System.currentTimeMillis()
                    // println"Changed"
                }
                if (count > 0) {
                    received += count
                }

                //println("$received : $expectedNum")
            }
            //println("${received.class} : ${expectedNum.class}")
            if (received == expectedNum) {
                Thread.sleep(100)
                break
            }
            if (noRecentMessage && (System.currentTimeMillis() - changedTime) > max_message_wait) {
                Thread.sleep(100)
                break
            }

            noRecentMessage = true
            received = 0
            Thread.sleep(100)
        }
        def endTime = System.currentTimeMillis()
        if (noRecentMessage) {
            print "Took ${(endTime - startTime) / 1000} seconds for readers to recieve all messages\n"
        } else {
            println "Readers did not recieve all messages in the configured max_message_wait of ${MAX_TIME_BETWEEN_MESSAGE} seconds"
        }
    }

    static waitForServerActive(Server server){
        def startTime = System.currentTimeMillis()
        def endTime = 0
        def change= 0
        while(!server.allActive()){
            Thread.sleep(500)
            endTime = System.currentTimeMillis()
            change  = endTime - startTime / 1000
            if(change > 5){
                println "Server not active in 5 seconds"
                break
            }

        }

        println "Server active in $change seconds"

    }
}
