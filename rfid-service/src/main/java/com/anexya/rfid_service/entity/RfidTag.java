package com.anexya.rfid_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(
        name = "rfid_tag",
        indexes = {
                @Index(name = "idx_rfid_tag_epc", columnList = "epc"),
                @Index(name = "idx_rfid_tag_tid", columnList = "tid"),
                @Index(name = "idx_rfid_tag_site_read_at", columnList = "site_name, read_at")
        })
public class RfidTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", nullable = false, length = 100)
    private String siteName;

    @Column(name = "epc", nullable = false, length = 64)
    private String epc;

    @Column(name = "tid", nullable = false, length = 64)
    private String tid;

    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Column(name = "rssi", nullable = false)
    private Integer rssi;

    @Column(name = "read_at", nullable = false)
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getEpc() { return epc; }
    public void setEpc(String epc) { this.epc = epc; }

    public String getTid() { return tid; }
    public void setTid(String tid) { this.tid = tid; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Integer getRssi() { return rssi; }
    public void setRssi(Integer rssi) { this.rssi = rssi; }

    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
