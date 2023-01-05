# Akka Streams kafka Consumer App

# Recommended tools for quick setup: 
- kafka: https://towardsdatascience.com/how-to-install-apache-kafka-using-docker-the-easy-way-4ceb00817d8b
- offset explorer: https://www.kafkatool.com/download.html

# Considerations

- kafka server version: 2.8.1

# Alpakka Kafka Stream Info:

- version 2.0: https://doc.akka.io/docs/alpakka-kafka/2.0.0/consumer.html

# Run docker image in docker
- docker-compose -f kafka-docker-compose.yml up
- default topics created: TestTopic1, TestTopic2 (check the docker-compose file)

# Connections info
- Zookeeper port: 2181