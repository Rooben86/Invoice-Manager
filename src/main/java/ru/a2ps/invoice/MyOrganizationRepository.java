package ru.a2ps.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MyOrganizationRepository extends JpaRepository<MyOrganization, Long> {
}