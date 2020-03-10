/*******************************************************************************
 * Copyright (c) 2015-2019 Skymind, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ******************************************************************************/

package org.deeplearning4j.examples.rl4j;

import java.io.IOException;

import org.deeplearning4j.api.storage.StatsStorage;
import org.deeplearning4j.rl4j.space.Box;
import org.deeplearning4j.rl4j.learning.sync.qlearning.QLearning;
import org.deeplearning4j.rl4j.learning.sync.qlearning.discrete.QLearningDiscreteDense;
import org.deeplearning4j.rl4j.mdp.gym.GymEnv;
import org.deeplearning4j.rl4j.network.dqn.DQNFactoryStdDense;
import org.deeplearning4j.rl4j.policy.DQNPolicy;
import org.deeplearning4j.rl4j.space.DiscreteSpace;
import org.deeplearning4j.rl4j.util.DataManager;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.stats.StatsListener;
import org.deeplearning4j.ui.storage.InMemoryStatsStorage;
import org.nd4j.linalg.learning.config.Adam;

import java.util.logging.Logger;


/**
 * @author rubenfiszel (ruben.fiszel@epfl.ch) on 8/11/16.
 *
 * Main example for Cartpole DQN
 *
 * **/
public class Cartpole
{

    public static void main(String[] args) throws IOException {
        cartPole();
        loadCartpole();
    }

    public static void cartPole() throws IOException {

        //record the training data in rl4j-data in a new folder (save)
        DataManager manager = new DataManager(true);

        StatsStorage statsStorage = new InMemoryStatsStorage();

        QLearning.QLConfiguration QLearningConfig = QLearning.QLConfiguration.builder()
            .gamma(0.95)
            .minEpsilon(0.01)
            .epsilonNbStep(1000)
            .build();

        DQNFactoryStdDense.Configuration cartpoleNetConfig = DQNFactoryStdDense.Configuration.builder()
            //.l2(0.001)
            .updater(new Adam(0.001))
            .numHiddenNodes(24)
            .numLayer(3)
            .listener(new StatsListener(statsStorage))
            .build();


        UIServer uiServer = UIServer.getInstance();
        uiServer.attach(statsStorage);

        //define the mdp from gym (name, render)
        GymEnv<Box, Integer, DiscreteSpace> mdp = null;
        try {
            mdp = new GymEnv("CartPole-v0", false, false);
        } catch (RuntimeException e){
            System.out.print("To run this example, download and start the gym-http-api repo found at https://github.com/openai/gym-http-api.");
        }
        //define the training
        QLearningDiscreteDense<Box> dql = new QLearningDiscreteDense(mdp, cartpoleNetConfig, QLearningConfig, manager);

        //train
        dql.train();

        //get the final policy
        DQNPolicy<Box> pol = dql.getPolicy();

        //serialize and save (serialization showcase, but not required)
        pol.save("/tmp/pol1");

        //close the mdp (close http)
        mdp.close();


    }


    public static void loadCartpole() throws IOException {

        //showcase serialization by using the trained agent on a new similar mdp (but render it this time)

        //define the mdp from gym (name, render)
        GymEnv mdp2 = new GymEnv("CartPole-v0", true, false);

        //load the previous agent
        DQNPolicy<Box> pol2 = DQNPolicy.load("/tmp/pol1");

        //evaluate the agent
        double rewards = 0;
        for (int i = 0; i < 1000; i++) {
            mdp2.reset();
            double reward = pol2.play(mdp2);
            rewards += reward;
            Logger.getAnonymousLogger().info("Reward: " + reward);
        }

        Logger.getAnonymousLogger().info("average: " + rewards/1000);

    }
}
