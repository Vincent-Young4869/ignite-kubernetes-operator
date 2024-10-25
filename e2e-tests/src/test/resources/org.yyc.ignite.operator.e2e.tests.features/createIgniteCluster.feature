Feature: Ignite user creates an IgniteResource through operator

  @teardownIgniteResource
  @resourceName:test_create_single_cluster
  Scenario: Create an IgniteResource if it doesn't exist
    Given There is no IgniteResource with name "test_create_single_cluster"
    When Create an IgniteResource with name "test_create_single_cluster"
    And Sleep for 120 seconds
    Then Ignite user should observe "ACTIVE_RUNNING" status for IgniteResource "test_create_single_cluster"
