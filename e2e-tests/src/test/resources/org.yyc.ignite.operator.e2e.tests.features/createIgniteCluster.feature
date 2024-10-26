Feature: Ignite user creates an IgniteResource through the operator

  @cleanupIgniteResource
  @resourceName:test-create-single-cluster
  Scenario: Create an IgniteResource if it doesn't exist
    Given There is no IgniteResource with name "test-create-single-cluster"
    When Create an IgniteResource with name "test-create-single-cluster"
    And Sleep for 15 seconds
    Then Ignite user should observe "INACTIVE_RUNNING" status for IgniteResource "test-create-single-cluster"
