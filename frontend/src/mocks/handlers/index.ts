import { assessmentsHandlers } from "./assessments.handlers";
import { athletesHandlers } from "./athletes.handlers";
import { authHandlers } from "./auth.handlers";
import { notificationsHandlers } from "./notifications.handlers";
import { nutritionHandlers } from "./nutrition.handlers";
import { trainingHandlers } from "./training.handlers";

export const handlers = [
  ...authHandlers,
  ...athletesHandlers,
  ...assessmentsHandlers,
  ...nutritionHandlers,
  ...trainingHandlers,
  ...notificationsHandlers,
];
