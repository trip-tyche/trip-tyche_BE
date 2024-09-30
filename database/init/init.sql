-- MySQL dump 10.13  Distrib 8.0.38, for macos14 (arm64)
--
-- Host: ec2-13-125-15-154.ap-northeast-2.compute.amazonaws.com    Database: feeling_memory
-- ------------------------------------------------------
-- Server version	8.0.39

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `media_file`
--

DROP TABLE IF EXISTS `media_file`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `media_file`
(
    `media_file_id` bigint       NOT NULL AUTO_INCREMENT,
    `latitude` double DEFAULT NULL,
    `longitude` double DEFAULT NULL,
    `media_key`     varchar(255) NOT NULL,
    `media_link`    varchar(255) DEFAULT NULL,
    `media_type`    varchar(50)  DEFAULT NULL,
    `record_date`   date         DEFAULT NULL,
    `pin_point_id`  bigint       NOT NULL,
    `trip_id`       bigint       NOT NULL,
    PRIMARY KEY (`media_file_id`),
    KEY             `FK8pl16dtj8fjghojtvtx22l2cl` (`pin_point_id`),
    KEY             `FK1b67ilppmsbjgwu19cxq6me2y` (`trip_id`),
    CONSTRAINT `FK1b67ilppmsbjgwu19cxq6me2y` FOREIGN KEY (`trip_id`) REFERENCES `trip` (`trip_id`),
    CONSTRAINT `FK8pl16dtj8fjghojtvtx22l2cl` FOREIGN KEY (`pin_point_id`) REFERENCES `pin_point` (`pin_point_id`)
) ENGINE=InnoDB AUTO_INCREMENT=19 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `media_file`
--

LOCK
TABLES `media_file` WRITE;
/*!40000 ALTER TABLE `media_file` DISABLE KEYS */;
INSERT INTO `media_file`
VALUES (1, 41.390299999999996, 2.1676444444444445,
        'uploads/1/d89aa41d-adec-4ff9-8785-5fb325d45f14_IMG_2121.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/d89aa41d-adec-4ff9-8785-5fb325d45f14_IMG_2121.JPG',
        'image/jpeg', '2023-07-20', 1, 1),
       (2, 41.39061111111111, 2.166447222222222,
        'uploads/1/5eb81059-bd04-420c-8e24-c3a61aada343_IMG_2127.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/5eb81059-bd04-420c-8e24-c3a61aada343_IMG_2127.JPG',
        'image/jpeg', '2023-07-20', 1, 1),
       (3, 41.39180833333333, 2.1651249999999997,
        'uploads/1/c6655332-c1c4-4d93-b30f-8c0a08da6503_IMG_2162.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/c6655332-c1c4-4d93-b30f-8c0a08da6503_IMG_2162.JPG',
        'image/jpeg', '2023-07-20', 1, 1),
       (4, 41.40457222222222, 2.1758416666666665,
        'uploads/1/a88729ca-6b8e-46dd-8d7d-54661d7f0f18_IMG_2946.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/a88729ca-6b8e-46dd-8d7d-54661d7f0f18_IMG_2946.JPG',
        'image/jpeg', '2023-07-26', 2, 1),
       (5, 41.391844444444445, 2.1650638888888887,
        'uploads/1/3ce2d8fa-97f1-44fb-9681-873624e1a2d3_IMG_4275.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/3ce2d8fa-97f1-44fb-9681-873624e1a2d3_IMG_4275.JPG',
        'image/jpeg', '2023-07-23', 1, 1),
       (6, 41.40114166666667, 2.18645,
        'uploads/1/9b1d883f-20cf-4b87-9b11-aa16aa1b0fa8_IMG_6074.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/9b1d883f-20cf-4b87-9b11-aa16aa1b0fa8_IMG_6074.JPG',
        'image/jpeg', '2023-07-22', 2, 1),
       (7, 41.401180555555555, 2.1864555555555554,
        'uploads/1/b0c69c78-cebc-4e4d-b69a-e7cda7ab4c31_IMG_6077.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/b0c69c78-cebc-4e4d-b69a-e7cda7ab4c31_IMG_6077.JPG',
        'image/jpeg', '2023-07-22', 2, 1),
       (8, 41.40105555555555, 2.186330555555555,
        'uploads/1/4e2c4030-a5ab-47a6-af2a-f1c010fee0bb_IMG_6119.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/4e2c4030-a5ab-47a6-af2a-f1c010fee0bb_IMG_6119.JPG',
        'image/jpeg', '2023-07-23', 2, 1),
       (9, 41.401019444444444, 2.1863527777777776,
        'uploads/1/47d74cce-0706-475f-911e-364ef418d578_IMG_6124.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/47d74cce-0706-475f-911e-364ef418d578_IMG_6124.JPG',
        'image/jpeg', '2023-07-23', 2, 1),
       (10, 41.401022222222224, 2.186330555555555,
        'uploads/1/d805c310-8564-4217-a9eb-91a33062cf5d_IMG_6125.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/d805c310-8564-4217-a9eb-91a33062cf5d_IMG_6125.JPG',
        'image/jpeg', '2023-07-23', 2, 1),
       (11, 41.40102777777778, 2.186319444444444,
        'uploads/1/93650fb1-f93b-45b3-9334-4d0239aa4aad_IMG_6126.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/93650fb1-f93b-45b3-9334-4d0239aa4aad_IMG_6126.JPG',
        'image/jpeg', '2023-07-23', 2, 1),
       (12, 41.389377777777774, 2.1863777777777775,
        'uploads/1/3ab622b6-a57d-4c53-82ea-3289e9c469f2_IMG_6208.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/3ab622b6-a57d-4c53-82ea-3289e9c469f2_IMG_6208.JPG',
        'image/jpeg', '2023-07-23', 3, 1),
       (13, 41.38937222222222, 2.186372222222222,
        'uploads/1/d02f32aa-f4e6-4c28-b466-6f58f7cba64d_IMG_6212.JPG',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/1/d02f32aa-f4e6-4c28-b466-6f58f7cba64d_IMG_6212.JPG',
        'image/jpeg', '2023-07-23', 3, 1),
       (14, 41.159478, -8.66046900008424,
        'uploads/2/931edcf8-e477-4e78-a9e5-496fff72638e_IMG_3037.jpg',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/2/931edcf8-e477-4e78-a9e5-496fff72638e_IMG_3037.jpg',
        'image/jpeg', '2023-07-29', 4, 2),
       (15, 41.15148557203464, -8.632421019170579,
        'uploads/2/06fadb5b-74ce-45ef-a45d-6edf0fabca07_IMG_3038.jpg',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/2/06fadb5b-74ce-45ef-a45d-6edf0fabca07_IMG_3038.jpg',
        'image/jpeg', '2023-07-29', 5, 2),
       (16, 41.155915, -8.660399, 'uploads/2/f82f7dd9-453c-439f-ad89-a334589436f4_IMG_3041.jpg',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/2/f82f7dd9-453c-439f-ad89-a334589436f4_IMG_3041.jpg',
        'image/jpeg', '2023-07-29', 4, 2),
       (17, 41.157697999999996, -8.63151,
        'uploads/2/7d32c2e6-c5a3-4cac-b89e-622f6a5d2215_IMG_3042.jpg',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/2/7d32c2e6-c5a3-4cac-b89e-622f6a5d2215_IMG_3042.jpg',
        'image/jpeg', '2023-07-29', 5, 2),
       (18, 41.15812, -8.626605, 'uploads/2/bbc9db8a-7d72-4cd4-a947-3bbc40be760c_IMG_3043.jpg',
        'https://five-feeling-memory.s3.amazonaws.com/uploads/2/bbc9db8a-7d72-4cd4-a947-3bbc40be760c_IMG_3043.jpg',
        'image/jpeg', '2023-07-29', 5, 2);
/*!40000 ALTER TABLE `media_file` ENABLE KEYS */;
UNLOCK
TABLES;

--
-- Table structure for table `pin_point`
--

DROP TABLE IF EXISTS `pin_point`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pin_point`
(
    `pin_point_id` bigint NOT NULL AUTO_INCREMENT,
    `latitude` double DEFAULT NULL,
    `longitude` double DEFAULT NULL,
    `trip_id`      bigint NOT NULL,
    PRIMARY KEY (`pin_point_id`),
    KEY            `FKmqe4tgrpve6r4tu2fntkqxojp` (`trip_id`),
    CONSTRAINT `FKmqe4tgrpve6r4tu2fntkqxojp` FOREIGN KEY (`trip_id`) REFERENCES `trip` (`trip_id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pin_point`
--

LOCK
TABLES `pin_point` WRITE;
/*!40000 ALTER TABLE `pin_point` DISABLE KEYS */;
INSERT INTO `pin_point`
VALUES (1, 41.390299999999996, 2.1676444444444445, 1),
       (2, 41.40457222222222, 2.1758416666666665, 1),
       (3, 41.389377777777774, 2.1863777777777775, 1),
       (4, 41.159478, -8.66046900008424, 2),
       (5, 41.15148557203464, -8.632421019170579, 2);
/*!40000 ALTER TABLE `pin_point` ENABLE KEYS */;
UNLOCK
TABLES;

--
-- Table structure for table `trip`
--

DROP TABLE IF EXISTS `trip`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `trip`
(
    `trip_id`    bigint       NOT NULL AUTO_INCREMENT,
    `country`    varchar(255) NOT NULL,
    `end_date`   date         NOT NULL,
    `hashtags`   varchar(255) DEFAULT NULL,
    `start_date` date         NOT NULL,
    `trip_title` varchar(255) NOT NULL,
    `user_id`    bigint       NOT NULL,
    PRIMARY KEY (`trip_id`),
    KEY          `FKd8pbh44g1ci1797yixosxacb9` (`user_id`),
    CONSTRAINT `FKd8pbh44g1ci1797yixosxacb9` FOREIGN KEY (`user_id`) REFERENCES `user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `trip`
--

LOCK
TABLES `trip` WRITE;
/*!40000 ALTER TABLE `trip` DISABLE KEYS */;
INSERT INTO `trip`
VALUES (1, '🇬🇧 영국', '2024-10-17', '베스트프렌즈,도전,행복한시간', '2024-10-14', '부드럽죠', 1),
       (2, '🇯🇵 일본', '2024-10-08', '베스트프렌즈,소소한두려움', '2024-10-06', '텍스쳐가 없잖아요', 1);
/*!40000 ALTER TABLE `trip` ENABLE KEYS */;
UNLOCK
TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user`
(
    `user_id`        bigint       NOT NULL AUTO_INCREMENT,
    `provider`       varchar(255) NOT NULL,
    `user_email`     varchar(255) NOT NULL,
    `user_name`      varchar(255) NOT NULL,
    `user_nick_name` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `UKj09k2v8lxofv2vecxu2hde9so` (`user_email`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK
TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user`
VALUES (1, 'google', 'redhero8830@gmail.com', 'Mark Kwon', '선경 숏기스트');
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK
TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2024-10-01  1:09:51
