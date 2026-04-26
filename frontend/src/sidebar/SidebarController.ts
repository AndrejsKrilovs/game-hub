class SidebarController {
  control = (eventBus: EventBus, root: HTMLElement) => {
    const startBtn = root.querySelector<HTMLButtonElement>("[data-start]")!
    const endBtn = root.querySelector<HTMLButtonElement>("[data-end]")!

    root.addEventListener("click", (e) => {
      const target = e.target as HTMLElement

      if (target.matches("[data-start]")) {
        eventBus.emit("OPEN_COLOR_PICKER")
      }
      if (target.matches("[data-end]")) {
        eventBus.emit("SHOW_END_CONFIRM")
      }
    })

    eventBus.on("OPEN_COLOR_PICKER", () => {
      startBtn.classList.add("hidden")
      endBtn.classList.remove("hidden")
      eventBus.emit("SHOW_COLOR_PICKER")
    })
    eventBus.on("WS:GAME_ENDED", (payload) => {
      startBtn.classList.remove("hidden")
      endBtn.classList.add("hidden")
      eventBus.emit("TOAST", payload)
    })
  }
}

export const sidebarController = new SidebarController()