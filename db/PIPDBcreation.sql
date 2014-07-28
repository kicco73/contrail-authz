SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL,ALLOW_INVALID_DATES';

CREATE SCHEMA IF NOT EXISTS `PipDB` ;
USE `PipDB` ;

-- -----------------------------------------------------
-- Table `PipDB`.`subscriptions`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `PipDB`.`subscriptions` (
  `id_subscription` INT NOT NULL AUTO_INCREMENT ,
  `owner` VARCHAR(45) NULL ,
  `attribute` VARCHAR(45) NULL ,
  `value` VARCHAR(45) NULL ,
  `subscriber` VARCHAR(45) NULL ,
  PRIMARY KEY (`id_subscription`) ,
  UNIQUE INDEX `id_subscription_UNIQUE` (`id_subscription` ASC) )
ENGINE = InnoDB;

USE `PipDB` ;


SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
