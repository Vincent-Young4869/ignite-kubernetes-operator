Feature: Ignite user creates an IgniteResource through the operator

  @cleanupIgniteResource
  @resourceName:test-create-single-cluster
  Scenario: Create an IgniteResource if it doesn't exist
    Given There is no IgniteResource with name "test-create-single-cluster"
    When Create an IgniteResource with name "test-create-single-cluster"
    And Sleep for 15 seconds
    Then Ignite user should observe "INACTIVE_RUNNING" status for IgniteResource "test-create-single-cluster"

  @cleanupIgniteResourcesFromSharedScenarioContext
  Scenario: Create multiple IgniteResources concurrently with different configurations
    Given the following IgniteResources configurations (they should not exist for now)
      | resourceName           | namespace  | memory            | replica | enablePersistence|
      | test-create-cluster-1  | e2e-test   | 120 * 1024 * 1024 |       1 |             true |
      | test-create-cluster-2  | e2e-test-2 | 110 * 1024 * 1024 |       2 |            false |
      | test-create-cluster-3  | e2e-test-3 | 100 * 1024 * 1024 |       1 |             true |
    When Create IgniteResources concurrently with these configurations
    And Sleep for 30 seconds
    Then Ignite user should observe healthy status for all IgniteResources with correct configurations respectively