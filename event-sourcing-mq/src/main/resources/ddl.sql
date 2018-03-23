CREATE TABLE `event`(
  `id` CHAR(36) NOT NULL,
  `event_type` VARCHAR(100),
  `model_name` VARCHAR(100),
  `model_id` CHAR(36),
  `created_time` BIGINT(11),
  PRIMARY KEY (`id`)
) ENGINE=INNODB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `foo`(
  `id` CHAR(36) NOT NULL,
  `name` VARCHAR(100),
  PRIMARY KEY (`id`)
) ENGINE=INNODB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `bar`(
  `id` CHAR(36) NOT NULL,
  `name` VARCHAR(100),
  PRIMARY KEY (`id`)
) ENGINE=INNODB CHARSET=utf8 COLLATE=utf8_general_ci;
