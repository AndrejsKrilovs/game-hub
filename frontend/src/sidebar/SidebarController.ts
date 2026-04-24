class SidebarController {
  control = (eventBus: EventBus, root: HTMLElement) => {
		eventBus.on("START_GAME", () => {
			root.querySelector(".btn-end").classList.remove("hidden")
			root.querySelector(".btn-start").classList.add("hidden")
		})
    eventBus.on("END_GAME", () => {
			root.querySelector(".btn-end").classList.add("hidden")
      root.querySelector(".btn-start").classList.remove("hidden")
		})
  }
}

export const sidebarController = new SidebarController()