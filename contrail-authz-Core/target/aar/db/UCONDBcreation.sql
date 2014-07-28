SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

CREATE SCHEMA IF NOT EXISTS `UconDB` DEFAULT CHARACTER SET latin1 COLLATE latin1_swedish_ci ;
USE `UconDB` ;

-- -----------------------------------------------------
-- Table `UconDB`.`sessions`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `UconDB`.`sessions` (
  `session_key` INT NOT NULL AUTO_INCREMENT ,
  `xacml_request` LONGTEXT NULL ,
  `replyTo` VARCHAR(255) NULL ,
  `session_status` TEXT NULL ,
  `lastReevaluation` TIMESTAMP NULL ,
  `messageId` VARCHAR(255) NULL ,
  `sessionId` VARCHAR(255) NULL ,
  PRIMARY KEY (`session_key`) ,
  UNIQUE INDEX `session_key_UNIQUE` (`session_key` ASC) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `UconDB`.`attribute`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `UconDB`.`attribute` (
  `attribute_key` INT NOT NULL AUTO_INCREMENT ,
  `category` TEXT NULL ,
  `type` TEXT NULL ,
  `xacml_attribute_id` TEXT NULL ,
  `value` TEXT NULL ,
  `issuer` TEXT NULL ,
  `lastChange` TIMESTAMP NULL ,
  `holder` TEXT NULL ,
  PRIMARY KEY (`attribute_key`) ,
  UNIQUE INDEX `attribute_key_UNIQUE` (`attribute_key` ASC) )
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `UconDB`.`attr_per_session`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `UconDB`.`attr_per_session` (
  `session_key` INT NOT NULL ,
  `attribute_key` INT NOT NULL ,
  PRIMARY KEY (`session_key`, `attribute_key`) ,
  INDEX `fk_attr_per_session_sessions` (`session_key` ASC) ,
  INDEX `fk_attr_per_session_attribute1` (`attribute_key` ASC) ,
  CONSTRAINT `fk_attr_per_session_sessions`
    FOREIGN KEY (`session_key` )
    REFERENCES `UconDB`.`sessions` (`session_key` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION,
  CONSTRAINT `fk_attr_per_session_attribute1`
    FOREIGN KEY (`attribute_key` )
    REFERENCES `UconDB`.`attribute` (`attribute_key` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;


-- -----------------------------------------------------
-- Table `UconDB`.`retrieval_policy`
-- -----------------------------------------------------
CREATE  TABLE IF NOT EXISTS `UconDB`.`retrieval_policy` (
  `retrieval_policy_id` BIGINT NOT NULL ,
  `attribute_key` INT NOT NULL ,
  `lastRetrieval` TIMESTAMP NULL ,
  PRIMARY KEY (`retrieval_policy_id`, `attribute_key`) ,
  INDEX `fk_retrieval_policy_attribute1` (`attribute_key` ASC) ,
  CONSTRAINT `fk_retrieval_policy_attribute1`
    FOREIGN KEY (`attribute_key` )
    REFERENCES `UconDB`.`attribute` (`attribute_key` )
    ON DELETE NO ACTION
    ON UPDATE NO ACTION)
ENGINE = InnoDB;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
