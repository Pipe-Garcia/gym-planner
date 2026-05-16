import { Navigate, Route, Routes } from "react-router-dom"
import { AppLayout } from "@/components/layout/AppLayout"
import { DashboardPage } from "@/pages/DashboardPage"
import { EditExercisePage } from "@/pages/exercises/EditExercisePage"
import { ExerciseDetailPage } from "@/pages/exercises/ExerciseDetailPage"
import { ExercisesListPage } from "@/pages/exercises/ExercisesListPage"
import { NewExercisePage } from "@/pages/exercises/NewExercisePage"
import { LoginPage } from "@/pages/LoginPage"
import { SettingsPage } from "@/pages/SettingsPage"
import { RoutinesListPage } from "@/pages/routines/RoutinesListPage"
import { EditStudentPage } from "@/pages/students/EditStudentPage"
import { NewStudentPage } from "@/pages/students/NewStudentPage"
import { StudentDetailPage } from "@/pages/students/StudentDetailPage"
import { StudentsListPage } from "@/pages/students/StudentsListPage"
import { RoutineViewPage } from "@/pages/students/routines/RoutineViewPage"
import { NewRoutineWizardPage } from "@/pages/students/routines/NewRoutineWizardPage"
import { RoutineEditorPage } from "@/pages/students/routines/RoutineEditorPage"
import { TemplateDetailPage } from "@/pages/templates/TemplateDetailPage"
import { TemplateEditorPage } from "@/pages/templates/TemplateEditorPage"
import { TemplatesListPage } from "@/pages/templates/TemplatesListPage"
import { ProtectedRoute } from "@/routes/ProtectedRoute"

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<ProtectedRoute />}>
        <Route element={<AppLayout />}>
          <Route index element={<DashboardPage />} />
          <Route path="students" element={<StudentsListPage />} />
          <Route path="students/new" element={<NewStudentPage />} />
          <Route path="students/:id" element={<StudentDetailPage />} />
          <Route path="students/:id/edit" element={<EditStudentPage />} />
          <Route path="students/:studentId/routines/new" element={<NewRoutineWizardPage />} />
          <Route path="students/:studentId/routines/:routineId" element={<RoutineViewPage />} />
          <Route path="students/:studentId/routines/:routineId/edit" element={<RoutineEditorPage />} />
          <Route path="routines" element={<RoutinesListPage />} />
          <Route path="exercises" element={<ExercisesListPage />} />
          <Route path="exercises/new" element={<NewExercisePage />} />
          <Route path="exercises/:id" element={<ExerciseDetailPage />} />
          <Route path="exercises/:id/edit" element={<EditExercisePage />} />
          <Route path="templates" element={<TemplatesListPage />} />
          <Route path="templates/new" element={<TemplateEditorPage />} />
          <Route path="templates/:id" element={<TemplateDetailPage />} />
          <Route path="templates/:id/edit" element={<TemplateEditorPage />} />
          <Route path="settings" element={<SettingsPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
