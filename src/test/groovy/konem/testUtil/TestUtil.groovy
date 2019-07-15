package konem.testUtil

import konem.data.json.KonemMessage
import konem.data.json.KonemMessageSerializer
import konem.netty.stream.Receiver
import konem.netty.stream.client.Client


class TestUtil {

    final static int MAX_CONNECT_TIME = 5
    final static int MAX_DISCONNECT_TIME = 5
    final static int MAX_TIME_BETWEEN_MESSAGE = 5000
    final static KonemMessageSerializer serializer = new KonemMessageSerializer()

    public static void ensureDisconnected(def clientList, max_disconnect_time = MAX_DISCONNECT_TIME) {
        def startTime = System.currentTimeMillis()
        boolean allInActive = true
        while (true) {
            allInActive = true
            clientList.each { Client client ->
                if (client.isActive()) {
                    allInActive = false
                }
            }

            if (allInActive) {
                break
            }
            if((System.currentTimeMillis() - startTime) / 1000 > max_disconnect_time ) break

            Thread.sleep(100)
        }
        def endTime = System.currentTimeMillis()

        if(allInActive){
            println "Took ${(endTime - startTime) / 1000L} seconds for ${clientList.size()} clients to disconnect"
        }
        else {
            def activeCount = clientList.findAll{ it.isActive() }
            println "${activeCount.size()} of ${clientList.size()} clients did not disconnect in the configured disconnect timeout of ${max_disconnect_time} seconds"
        }
    }

    public static void ensureClientsActive(def clientList, max_connect_time = MAX_CONNECT_TIME) {
        def startTime = System.currentTimeMillis()
        boolean allActive = true
        while (true) {
            allActive = true
            clientList.each { Client client ->
                if (!client.isActive()) {
                    allActive = false
                }
            }

            if (allActive) {
                break
            }
            if((System.currentTimeMillis() - startTime) / 1000 > max_connect_time ){
                break
            }
            Thread.sleep(500)
        }
        def endTime = System.currentTimeMillis()
        if(allActive){
            println "Took ${(endTime - startTime) / 1000L} seconds for ${clientList.size()} clients to connect"
        }
        else {
            def inactiveCount = clientList.findAll{ !it.isActive() }
            println "${inactiveCount.size()} of ${clientList.size()} clients did not connect in the configured connection timeout of ${max_connect_time} seconds"
        }

    }


    static KonemMessage createKonemMessage(String json){
        return serializer.toKonemMessage(json)
    }

    public static waitForAllMessges(def readerList, max_message_wait = MAX_TIME_BETWEEN_MESSAGE){
        def messageCountMap = [:]
        for(Receiver reader in readerList){
            messageCountMap.put(reader,reader.messageCount)
        }
        def startTime = System.currentTimeMillis()
        def stillRecieving = true
        while(true){
            stillRecieving = false
            for(Receiver reader in readerList){
                def count = messageCountMap.get(reader)
                if(count != reader.messageCount){
                    messageCountMap.put(reader,reader.messageCount)
                    stillRecieving = true
                }
            }
            if(!stillRecieving && (System.currentTimeMillis() - startTime)  > max_message_wait) {
                break
            }
            if((System.currentTimeMillis() - startTime) / 1000 > max_message_wait ){
                break
            }
            Thread.sleep(500)
        }
        def endTime = System.currentTimeMillis()
        if(!stillRecieving){
            println "Took ${(endTime - startTime) / 1000} seconds for readers to recieve all messages"
        }
        else {
            println "Readers did not recieve all messages in the configured max_message_wait of ${MAX_TIME_BETWEEN_MESSAGE} seconds"
        }
    }
}
