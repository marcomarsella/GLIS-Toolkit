-- phpMyAdmin SQL Dump
-- version 4.5.4.1deb2ubuntu2.1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Generation Time: Feb 23, 2019 at 07:53 PM
-- Server version: 5.7.25-0ubuntu0.16.04.2
-- PHP Version: 7.0.33-0ubuntu0.16.04.1

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `glistk`
--

-- --------------------------------------------------------

--
-- Table structure for table `actors`
--

CREATE TABLE `actors` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `sample_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` char(2) COLLATE utf8mb4_unicode_ci NOT NULL,
  `wiews` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pid` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `country` char(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `identifiers`
--

CREATE TABLE `identifiers` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `sample_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `names`
--

CREATE TABLE `names` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `sample_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name_type` char(2) COLLATE utf8mb4_unicode_ci NOT NULL,
  `name` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `pgrfas`
--

CREATE TABLE `pgrfas` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `operation` varchar(8) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sample_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `processed` char(1) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sample_doi` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `date` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `hold_wiews` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hold_pid` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hold_name` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hold_address` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `hold_country` char(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `method` char(4) COLLATE utf8mb4_unicode_ci NOT NULL,
  `genus` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `species` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sp_auth` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `subtaxa` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `st_auth` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `bio_status` char(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `mls_status` varchar(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `prov_sid` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `provenance` char(3) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_sid` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_miss_id` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_site` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_lat` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_lon` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_uncert` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_datum` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_georef` varchar(16) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `coll_elevation` int DEFAULT NULL,
  `coll_date` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `ancestry` text COLLATE utf8mb4_unicode_ci,
  `coll_source` char(2) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `historical` char(1) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `progdois` varchar(256) COLLATE utf8mb4_unicode_ci DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci ROW_FORMAT=COMPACT;

-- --------------------------------------------------------

--
-- Table structure for table `results`
--

CREATE TABLE `results` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `operation` varchar(16) COLLATE utf8mb4_unicode_ci NOT NULL,
  `genus` varchar(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `sample_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `doi` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `result` varchar(2) COLLATE utf8mb4_unicode_ci NOT NULL,
  `error` text COLLATE utf8mb4_unicode_ci
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- --------------------------------------------------------

--
-- Table structure for table `targets`
--

CREATE TABLE `targets` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `sample_id` varchar(128) COLLATE utf8mb4_unicode_ci NOT NULL,
  `value` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL,
  `tkws` varchar(256) COLLATE utf8mb4_unicode_ci NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


--
-- Indexes for dumped tables
--

--
-- Indexes for table `actors`
--
ALTER TABLE `actors`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_names_pgrfas` (`sample_id`);

--
-- Indexes for table `identifiers`
--
ALTER TABLE `identifiers`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk1_identifiers_pgrfas` (`sample_id`);

--
-- Indexes for table `names`
--
ALTER TABLE `names`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk1_names_pgrfas` (`sample_id`);

--
-- Indexes for table `pgrfas`
--
ALTER TABLE `pgrfas`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `pgrfas_id` (`sample_id`);

--
-- Indexes for table `results`
--
ALTER TABLE `results`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `targets`
--
ALTER TABLE `targets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_targets_pgrfas` (`sample_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `actors`
--
ALTER TABLE `actors`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `identifiers`
--
ALTER TABLE `identifiers`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `names`
--
ALTER TABLE `names`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `pgrfas`
--
ALTER TABLE `pgrfas`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `results`
--
ALTER TABLE `results`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- AUTO_INCREMENT for table `targets`
--
ALTER TABLE `targets`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT;
--
-- Constraints for dumped tables
--

--
-- Constraints for table `actors`
--
ALTER TABLE `actors`
  ADD CONSTRAINT `fk_actors_pgrfas` FOREIGN KEY (`sample_id`) REFERENCES `pgrfas` (`sample_id`),
  ADD CONSTRAINT `fk_identifiers_pgrfas` FOREIGN KEY (`sample_id`) REFERENCES `pgrfas` (`sample_id`),
  ADD CONSTRAINT `fk_names_pgrfas` FOREIGN KEY (`sample_id`) REFERENCES `pgrfas` (`sample_id`);

--
-- Constraints for table `identifiers`
--
ALTER TABLE `identifiers`
  ADD CONSTRAINT `fk1_identifiers_pgrfas` FOREIGN KEY (`sample_id`) REFERENCES `pgrfas` (`sample_id`);

--
-- Constraints for table `names`
--
ALTER TABLE `names`
  ADD CONSTRAINT `fk1_names_pgrfas` FOREIGN KEY (`sample_id`) REFERENCES `pgrfas` (`sample_id`);

--
-- Constraints for table `targets`
--
ALTER TABLE `targets`
  ADD CONSTRAINT `fk_targets_pgrfas` FOREIGN KEY (`sample_id`) REFERENCES `pgrfas` (`sample_id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
