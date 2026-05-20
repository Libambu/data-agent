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
        public static final String INTERRUPT_NODE = CONFIRM;

        private Node() {
        }
    }

    public static final class StateKey {
        public static final String INPUT = "input";
        public static final String SCENE = "scene";
        public static final String SCENE_LABEL = "sceneLabel";
        public static final String DRAFT = "draft";
        public static final String FINAL_OUTPUT = "finalOutput";
        public static final String CONFIRMATION_APPROVED = "confirmationApproved";
        public static final String CONFIRMATION_FEEDBACK = "confirmationFeedback";

        private StateKey() {
        }
    }

    public static final class Scene {
        public static final String TRAVEL = "travel";
        public static final String STUDY = "study";

        private Scene() {
        }
    }

    /**
     * {
            "jsonrpc": "2.0",
            "id": "req-003",
            "method": "message/stream",
            "params": {
                "message": {
                "kind": "message",
                "messageId": "msg-003",
                "role": "user",
                "taskId": "task-123",
                "contextId": "context-abc",
                "parts": [
                    {
                    "kind": "text",
                    "text": "行程太赶了，减少景点"
                    }
                ],
                "metadata": {
                    "confirmationApproved": false,
                    "confirmationFeedback": "行程太赶了，减少景点"
                }
                }
            }
        }
     */
    public static final class MessageMetadataKey {
        //用户是否同意当前生成的草稿。
        public static final String CONFIRMATION_APPROVED = "confirmationApproved";
        //用户对当前草稿的反馈意见。
        public static final String CONFIRMATION_FEEDBACK = "confirmationFeedback";

        private MessageMetadataKey() {
        }
    }
}
