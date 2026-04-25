class SidebarController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    const startBtn = root.querySelector<HTMLButtonElement>("[data-start]")!
    const endBtn = root.querySelector<HTMLButtonElement>("[data-end]")!

    root.addEventListener("click", (e) => {
      const target = e.target as HTMLElement

      if (target.matches("[data-start]")) {
        eventBus.emit("START_GAME")
      }
      if (target.matches("[data-end]")) {
        eventBus.emit("END_GAME")
      }
    })

    eventBus.on("START_GAME", () => {
      startBtn.classList.add("hidden")
      endBtn.classList.remove("hidden")
    })
    eventBus.on("END_GAME", () => {
      startBtn.classList.remove("hidden")
      endBtn.classList.add("hidden")
    })
    eventBus.on("OPEN_COLOR_PICKER", () => {
      console.log("open color picker (пока просто лог)")
    })
  }
}

export const sidebarController = new SidebarController()