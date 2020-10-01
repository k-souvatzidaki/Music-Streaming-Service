# Music Streaming Service

## About
A group project for a distributed systems course in uni. A simple music streaming service implemented in a client-broker-server model. This distributed application consists of 4 kinds of nodes, the **Publishers(or servers)**, the **Brokers**,the **Zookeeper**, and the **Consumers(or clients)**. The Publishers, the Zookeeper and the Brokers run **Java** apps on many computers (or a single computer but using different ports), while the Consumers run an **Android** application. The nodes communicate via network using **Java sockets**.

## Publisher-Broker-Consumer model description
Each node has a different purpose. Briefly, the **Publishers** store mp3 file data and forward them to the **Brokers**, each time a **Consumer** requests some song data. The Brokers, then, forward the requested data to the Consumers. **Load balancing** is achieved via the use of **many Brokers** . Each Broker is answering requests for a range of songs ,ex. a range of song name initials. A Broker answers Consumer requests for songs starting with A-C, another one for songs starting with D-F, etc. The distribution of song names to the Brokers is achieved with SHA-1 hashing, in a way similar to the **CHORD peer-to-peer protocol**.We used 3 brokers for this assignment. **Parallel programming using threads** is also used, so that **many Consumers can send requests for the same songs simultaneously**. 

## The Zookeeper
This node is used to synchronise the Brokers, so that all Brokers know which song requests to answer. The Zookeeper could possibly do more things as in the **Apache Kafka stream-processing software platform**, but we kept it simple for this project. 

## The Android app
The **Consumers** are running a simple Android application that can run both online or offline. If **online**, the app is initially requesting song information from the brokers and showing the song names in a scrollable list. The user can select an artist and a song, and the application instantly sends a request for the song to the right **Broker**. Then, the user can listen to the music with a simple **player**. Clients can also **download songs**. If **offline**, the user can listen to downloaded songs only.

## Datasets
The song datasets are copyright-free and can be downloaded [here](https://drive.google.com/drive/folders/1xzoojEgCdPV9i6jmeCrl1WIEZcdNsoVS?usp=sharing)

## Libraries
We used the [mp3agic](https://github.com/mpatric/mp3agic) library to read mp3 file tags. 

## How to run
Download the dataset from the link above, and place it in the **Event Delivery System** folder.
In the Event Delivery System/src/../Node.java file there are lists with the addresses of Publishers, Brokers, Zookeeper. We change the ip addresses manually.
We execute the main apps in the following order:
-> first Zookeeper.java.
-> then Broker1.java, Broker2.java, Broker3.java, in any order. Each of these files contains a main method, one for each Broker.
-> then Publisher1.java, Publisher2.java (main for Publishers), in any order.
-> finally, we run the Android application on a mobile phone or Android Studio VM, for the Consumer.

## Group
[Themelina Kouzoumpasi](https://github.com/themelinaKz)
[Lydia Athanasiou](https://github.com/lydia-ath)  
[Konstantina Souvatzidaki](https://github.com/k-souvatzidaki)
