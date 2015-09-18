package gov.nist.hit.core.hl7v2.repo;

import gov.nist.hit.core.domain.ConformanceProfile;
import gov.nist.hit.core.hl7v2.domain.HL7V2TestContext;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HL7V2TestContextRepository extends JpaRepository<HL7V2TestContext, Long> {

  @Query("select tc.conformanceProfile from TestContext tc where tc.id = :id")
  public ConformanceProfile findConformanceProfileByTestContextId(@Param("id") Long id);
}
