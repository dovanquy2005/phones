package com.fonestore.staff_api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "UK_staff_user", columnNames = "user_id")
})
public class StaffProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id",
            foreignKey = @ForeignKey(name = "fk_staff_user"))
    private User user;

    @Column(length = 100)
    private String position;

    @Column(length = 50)
    private String phone;

    @Column(length = 500)
    private String note;

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
