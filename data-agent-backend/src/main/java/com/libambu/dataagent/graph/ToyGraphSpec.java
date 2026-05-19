package com.libambu.dataagent.graph;

public final class ToyGraphSpec {

    public static final String NAME = "toy-branch-streaming-graph";

    private ToyGraphSpec() {
    }

    public static final class Node {
        public static final String ROUTE = "ROUTE_NODE";
        public static final String CONFIRM = "CONFIRM_NODE";
        public static final String TRAVEL_PLAN = "TRAVEL_PLAN_NODE";
        public static final String STUDY_PLAN = "STUDY_PLAN_NODE";
        public static final String WRAP_UP = "WRAP_UP_NODE";

        private Node() {
        }
    }

    public static final class StateKey {
        public static final String INPUT = "input";
        public static final String SCENE = "scene";
        public static final String SCENE_LABEL = "sceneLabel";
        public static final String DRAFT = "draft";
        public static final String FINAL_OUTPUT = "finalOutput";

        private StateKey() {
        }
    }

    public static final class Scene {
        public static final String TRAVEL = "travel";
        public static final String STUDY = "study";

        private Scene() {
        }
    }
}
