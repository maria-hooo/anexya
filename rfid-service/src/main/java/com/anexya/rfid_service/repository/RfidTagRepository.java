package com.anexya.rfid_service.repository;

import com.anexya.rfid_service.entity.RfidTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RfidTagRepository extends JpaRepository<RfidTag, Long>, JpaSpecificationExecutor<RfidTag> {
}
