import dashboardIcon from '@/assets/icons/dashboard.svg'
import teacherIcon from '@/assets/icons/teacher.svg'
import subjectsIcon from '@/assets/icons/subjects.svg'
import boardIcon from '@/assets/icons/board.svg'
import kanbanIcon from '@/assets/icons/kanban.svg'

export const NAV_ITEMS = [
  { label: 'Dashboard', routeName: 'dashboard', icon: dashboardIcon },
  { label: 'Professor', routeName: 'teacher', icon: teacherIcon },
  { label: 'Disciplinas', routeName: 'subjects', icon: subjectsIcon },
  { label: 'Mural Colaborativo', routeName: 'board', icon: boardIcon },
  { label: 'Kanban', routeName: 'kanban', icon: kanbanIcon },
]