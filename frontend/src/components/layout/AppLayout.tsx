import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { Outlet } from "react-router-dom"
import { getCurrentGym } from "@/api/gym"
import { Header } from "@/components/layout/Header"
import { Sidebar } from "@/components/layout/Sidebar"

export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)
  const { data: gym } = useQuery({
    queryKey: ["gym", "current"],
    queryFn: getCurrentGym,
  })

  return (
    <div className="min-h-screen bg-background lg:flex">
      <Sidebar open={sidebarOpen} onClose={() => setSidebarOpen(false)} />
      <div className="min-w-0 flex-1">
        <Header gymName={gym?.name} onMenuClick={() => setSidebarOpen(true)} />
        <main className="mx-auto w-full max-w-6xl px-4 py-6 lg:px-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
