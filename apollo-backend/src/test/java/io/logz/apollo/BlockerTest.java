package io.logz.apollo;

import io.logz.apollo.blockers.BlockerDefinition;
import io.logz.apollo.clients.ApolloTestAdminClient;
import io.logz.apollo.clients.ApolloTestClient;
import io.logz.apollo.exceptions.ApolloBlockedException;
import io.logz.apollo.helpers.Common;
import io.logz.apollo.helpers.ModelsGenerator;
import io.logz.apollo.helpers.StandaloneApollo;
import io.logz.apollo.models.DeployableVersion;
import io.logz.apollo.models.Environment;
import io.logz.apollo.models.Service;
import io.logz.apollo.scm.GithubConnector;
import org.junit.Test;

import javax.script.ScriptException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.logz.apollo.helpers.ModelsGenerator.createAndSubmitBlocker;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by roiravhon on 6/5/17.
 */
public class BlockerTest {

    @Test
    public void testEverythingBlocker() throws Exception {
        ApolloTestClient apolloTestClient = Common.signupAndLogin();
        ApolloTestAdminClient apolloTestAdminClient = Common.getAndLoginApolloTestAdminClient();

        Environment environment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Service service = ModelsGenerator.createAndSubmitService(apolloTestClient);
        DeployableVersion deployableVersion = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, service);

        BlockerDefinition blocker = createAndSubmitBlocker(apolloTestAdminClient, "unconditional", "{}", null, null);

        assertThatThrownBy(() -> ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, service ,deployableVersion)).isInstanceOf(ApolloBlockedException.class);

        blocker.setActive(false);
        apolloTestAdminClient.updateBlocker(blocker);

        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, service ,deployableVersion);
    }

    @Test
    public void testEnvironmentBlocker() throws Exception {
        ApolloTestClient apolloTestClient = Common.signupAndLogin();
        ApolloTestAdminClient apolloTestAdminClient = Common.getAndLoginApolloTestAdminClient();

        Environment blockedEnvironment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Environment okEnvironment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Service service = ModelsGenerator.createAndSubmitService(apolloTestClient);
        DeployableVersion deployableVersion = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, service);

        createAndSubmitBlocker(apolloTestAdminClient, "unconditional", "{}", blockedEnvironment, null);

        assertThatThrownBy(() -> ModelsGenerator.createAndSubmitDeployment(apolloTestClient, blockedEnvironment, service ,deployableVersion)).isInstanceOf(ApolloBlockedException.class);
        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, okEnvironment, service ,deployableVersion);
    }

    @Test
    public void testServiceBlocker() throws Exception {
        ApolloTestClient apolloTestClient = Common.signupAndLogin();
        ApolloTestAdminClient apolloTestAdminClient = Common.getAndLoginApolloTestAdminClient();

        Environment environment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Service blockedService = ModelsGenerator.createAndSubmitService(apolloTestClient);
        Service okService = ModelsGenerator.createAndSubmitService(apolloTestClient);
        DeployableVersion blockedDeployableVersion = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, blockedService);
        DeployableVersion okDeployableVersion = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, okService);

        createAndSubmitBlocker(apolloTestAdminClient, "unconditional", "{}", null, blockedService);

        assertThatThrownBy(() -> ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, blockedService ,blockedDeployableVersion)).isInstanceOf(ApolloBlockedException.class);
        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, okService ,okDeployableVersion);
    }

    @Test
    public void testSpecificBlocker() throws Exception {
        ApolloTestClient apolloTestClient = Common.signupAndLogin();
        ApolloTestAdminClient apolloTestAdminClient = Common.getAndLoginApolloTestAdminClient();

        Environment blockedEnvironment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Environment okEnvironment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Service blockedService = ModelsGenerator.createAndSubmitService(apolloTestClient);
        Service okService = ModelsGenerator.createAndSubmitService(apolloTestClient);
        DeployableVersion blockedDeployableVersion = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, blockedService);
        DeployableVersion okDeployableVersion = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, okService);

        createAndSubmitBlocker(apolloTestAdminClient, "unconditional", "{}", blockedEnvironment, blockedService);

        assertThatThrownBy(() -> ModelsGenerator.createAndSubmitDeployment(apolloTestClient, blockedEnvironment, blockedService ,blockedDeployableVersion)).isInstanceOf(ApolloBlockedException.class);
        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, blockedEnvironment, okService ,okDeployableVersion);
        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, okEnvironment, blockedService ,blockedDeployableVersion);
    }

    @Test
    public void testTimeBasedBlocker() throws Exception {
        ApolloTestClient apolloTestClient = Common.signupAndLogin();
        ApolloTestAdminClient apolloTestAdminClient = Common.getAndLoginApolloTestAdminClient();

        Environment environment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Service service = ModelsGenerator.createAndSubmitService(apolloTestClient);
        DeployableVersion deployableVersion = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, service);

        int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
        LocalTime twoMinutesFromNow = LocalTime.now().plusMinutes(2);
        LocalTime twoMinutesBeforeNow = LocalTime.now().minusMinutes(2);
        LocalTime threeMinutesFromNow = LocalTime.now().plusMinutes(3);

        BlockerDefinition blocker = createAndSubmitBlocker(apolloTestAdminClient, "timebased",
                getTimeBasedBlockerJsonConfiguration(dayOfWeek, twoMinutesBeforeNow, twoMinutesFromNow),
                environment, service);

        assertThatThrownBy(() -> ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, service ,deployableVersion)).isInstanceOf(ApolloBlockedException.class);

        blocker.setBlockerJsonConfiguration(getTimeBasedBlockerJsonConfiguration(dayOfWeek, twoMinutesFromNow, threeMinutesFromNow));
        apolloTestAdminClient.updateBlocker(blocker);

        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, service ,deployableVersion);
    }

    @Test
    public void testBranchBlocker() throws Exception {
        ApolloTestClient apolloTestClient = Common.signupAndLogin();
        ApolloTestAdminClient apolloTestAdminClient = Common.getAndLoginApolloTestAdminClient();

        Environment environment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Service service = ModelsGenerator.createAndSubmitService(apolloTestClient);

        // Create a deployable version from logzio public API repo (small public one)
        DeployableVersion deployableVersion = new DeployableVersion();
        deployableVersion.setGithubRepositoryUrl("https://github.com/logzio/public-api");
        deployableVersion.setGitCommitSha("c5265fcc8a73c9d5a79170b668c21c958b53a93e");
        deployableVersion.setServiceId(service.getId());

        deployableVersion.setId(apolloTestClient.addDeployableVersion(deployableVersion).getId());


        BlockerDefinition blocker = createAndSubmitBlocker(apolloTestAdminClient, "branch",
                getBranchBlockerJsonConfiguration("develop"),
                environment, service);

        assertThatThrownBy(() -> ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, service ,deployableVersion)).isInstanceOf(ApolloBlockedException.class);

        blocker.setBlockerJsonConfiguration(getBranchBlockerJsonConfiguration("master"));
        apolloTestAdminClient.updateBlocker(blocker);

        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, service ,deployableVersion);
    }

    @Test
    public void testConcurrencyBlocker() throws Exception {
        ApolloTestClient apolloTestClient = Common.signupAndLogin();
        ApolloTestAdminClient apolloTestAdminClient = Common.getAndLoginApolloTestAdminClient();

        Environment environment = ModelsGenerator.createAndSubmitEnvironment(apolloTestClient);
        Service serviceA = ModelsGenerator.createAndSubmitService(apolloTestClient);
        Service serviceB = ModelsGenerator.createAndSubmitService(apolloTestClient);

        DeployableVersion deployableVersionA = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, serviceA);
        DeployableVersion deployableVersionB = ModelsGenerator.createAndSubmitDeployableVersion(apolloTestClient, serviceB);

        List<Integer> excludedService = new ArrayList<>();

        BlockerDefinition blocker = createAndSubmitBlocker(apolloTestAdminClient, "concurrent",
                getConcurrencyBlockerJsonConfiguration(1, excludedService),
                environment, null);

        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, serviceA ,deployableVersionA);

        assertThatThrownBy(() -> ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, serviceB ,deployableVersionB)).isInstanceOf(ApolloBlockedException.class);

        excludedService.add(serviceB.getId());
        blocker.setBlockerJsonConfiguration(getConcurrencyBlockerJsonConfiguration(1, excludedService));
        apolloTestAdminClient.updateBlocker(blocker);
        ModelsGenerator.createAndSubmitDeployment(apolloTestClient, environment, serviceB ,deployableVersionB);
    }

    private String getTimeBasedBlockerJsonConfiguration(int dayOfWeek, LocalTime startDate, LocalTime endDate) {

        return "{\n" +
                "  \"startTimeUtc\": \"" + startDate.getHour() + ":" + startDate.getMinute() + "\",\n" +
                "  \"endTimeUtc\": \"" + endDate.getHour() + ":" + endDate.getMinute() + "\",\n" +
                "  \"daysOfTheWeek\": [\n" +
                "    "+ dayOfWeek +"\n" +
                "  ]\n" +
                "}";
    }

    private String getBranchBlockerJsonConfiguration(String branchName) {
        return "{\n" +
                "  \"branchName\": \"" + branchName + "\"\n" +
                "}";
    }

    private String getConcurrencyBlockerJsonConfiguration(int allowedConcurrentDeployment, List<Integer> excludeServices) {
        return "{\n" +
                "  \"allowedConcurrentDeployment\": \"" + allowedConcurrentDeployment + "\",\n" +
                "  \"excludeServices\":"+ excludeServices.toString() +
                "}";
    }
}