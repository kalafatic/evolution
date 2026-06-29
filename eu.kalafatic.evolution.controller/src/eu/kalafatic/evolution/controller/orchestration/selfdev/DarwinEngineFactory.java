//package eu.kalafatic.evolution.controller.orchestration.selfdev;
//
//import eu.kalafatic.evolution.controller.orchestration.AiService;
//import eu.kalafatic.evolution.controller.orchestration.TaskContext;
//import eu.kalafatic.evolution.controller.orchestration.util.ModeRecognizer;
//
///**
// * Factory for creating mode-specific Darwin engines.
// * Returns IDarwinEngine interface instances.
// */
//public class DarwinEngineFactory {
//
//    public static IDarwinEngine create(TaskContext context, IterationMemoryService memoryService,
//                                       AiService aiService) {
//
//        String mode = ModeRecognizer.determineMode(context);
//        context.log("[DARWIN_FACTORY] Creating Darwin Engine for mode: " + mode);
//
//        switch (mode) {
//            case "SELF_DEV":
//                context.log("[DARWIN_FACTORY] Creating Self-Dev Engine");
//                ISelfDevSupervisor supervisor = new MavenSelfDevSupervisor(context);
//                return new SelfDevEngine(context, memoryService, supervisor);
//
//            case "MEDIATED":
//                context.log("[DARWIN_FACTORY] Creating Mediated Engine");
//                return new MediatedEngine(context, memoryService);
//
//            case "CHAT":
//                context.log("[DARWIN_FACTORY] Creating Chat Engine");
//                return new ChatEngine(context, memoryService);
//
//            case "STANDARD":
//            default:
//                context.log("[DARWIN_FACTORY] Creating Standard Engine");
//                return new StandardEngine(context, memoryService);
//        }
//    }
//}