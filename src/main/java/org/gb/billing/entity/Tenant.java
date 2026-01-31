package org.gb.billing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents an organization or tenant in the multi-tenant SaaS system.
 * Each tenant is a logical grouping of users and their associated data.
 */
@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenants_name", columnList = "name"),
    @Index(name = "idx_tenants_active", columnList = "active")
})
@EntityListeners(AuditingEntityListener.class)
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tenant name is required")
    @Size(max = 255, message = "Tenant name must not exceed 255 characters")
    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false)
    private Boolean active = true;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_date")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @Column(nullable = false, name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "last_modified_by", length = 100)
    private String lastModifiedBy;

    // Constructors
    public Tenant() {
    }

    public Tenant(String name) {
        this.name = name;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getLastModifiedDate() {
        return lastModifiedDate;
    }

    public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    public void setLastModifiedBy(String lastModifiedBy) {
        this.lastModifiedBy = lastModifiedBy;
    }

    // equals, hashCode, toString
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return Objects.equals(id, tenant.id) && Objects.equals(name, tenant.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", active=" + active +
                ", createdDate=" + createdDate +
                ", lastModifiedDate=" + lastModifiedDate +
                '}';
    }
}
