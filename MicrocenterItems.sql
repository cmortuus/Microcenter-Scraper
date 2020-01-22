-- phpMyAdmin SQL Dump
-- version 4.6.6deb5
-- https://www.phpmyadmin.net/
--
-- Host: localhost
-- Generation Time: Jan 21, 2020 at 08:32 PM
-- Server version: 5.7.28-0ubuntu0.19.04.2
-- PHP Version: 7.2.24-0ubuntu0.19.04.2

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `MicrocenterItems`
--
CREATE DATABASE IF NOT EXISTS `MicrocenterItems` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci;
USE `MicrocenterItems`;

-- --------------------------------------------------------

--
-- Table structure for table `EmailsSent`
--

CREATE TABLE `EmailsSent` (
  `id` int(11) NOT NULL,
  `productName` varchar(128) NOT NULL,
  `openBoxPrice` double NOT NULL,
  `percentDifference` double NOT NULL,
  `store` varchar(32) NOT NULL,
  `name` varchar(32) NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Items`
--

CREATE TABLE `Items` (
  `id` int(11) NOT NULL,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `category` varchar(32) NOT NULL,
  `productName` varchar(256) NOT NULL,
  `url` varchar(512) NOT NULL,
  `normalPrice` double NOT NULL,
  `openBoxPrice` double NOT NULL,
  `percentDifference` double NOT NULL,
  `store` varchar(32) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `Searches`
--

CREATE TABLE `Searches` (
  `id` int(11) NOT NULL,
  `productName` varchar(256) DEFAULT NULL,
  `category` varchar(32) DEFAULT NULL,
  `store` varchar(32) DEFAULT NULL,
  `minPrice` double DEFAULT NULL,
  `maxPrice` double DEFAULT NULL,
  `percentDifference` double DEFAULT NULL,
  `name` varchar(32) NOT NULL,
  `email` varchar(128) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Searches`
--

INSERT INTO `Searches` (`id`, `productName`, `category`, `store`, `minPrice`, `maxPrice`, `percentDifference`, `name`, `email`) VALUES
(1, 'ryzen', NULL, NULL, NULL, NULL, NULL, 'ryzen', 'calebmorton98@gmail.com'),
(3, 'laptop', NULL, NULL, 100, NULL, 75, 'laptop75', 'calebmorton98@gmail.com');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `EmailsSent`
--
ALTER TABLE `EmailsSent`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `Items`
--
ALTER TABLE `Items`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `Searches`
--
ALTER TABLE `Searches`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `EmailsSent`
--
ALTER TABLE `EmailsSent`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2608;
--
-- AUTO_INCREMENT for table `Items`
--
ALTER TABLE `Items`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=44807;
--
-- AUTO_INCREMENT for table `Searches`
--
ALTER TABLE `Searches`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
