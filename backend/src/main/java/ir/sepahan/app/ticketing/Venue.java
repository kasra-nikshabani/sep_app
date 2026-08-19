package ir.sepahan.app.ticketing;

import ir.sepahan.app.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** چیدمان فیزیکی و ثابت سالن — طبق docs/database/erd-ticketing.md، بارها بین اجراهای مختلف استفاده می‌شود. */
@Entity
@Table(name = "venue", schema = "ticketing")
public class Venue extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String city;

    @Column
    private String address;

    protected Venue() {
        // JPA
    }

    public Venue(String name, String city, String address) {
        this.name = name;
        this.city = city;
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }
}
