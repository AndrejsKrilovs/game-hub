class ConfettiController {
  control = (eventBus: EventBus) => {
    eventBus.on("CONFETTI", () => this.launch())
  }

  private launch = () => {
    document.querySelector(".confetti-layer")?.remove()

    const layer = document.createElement("div")
    layer.className = "confetti-layer"

    const count = 90

    for (let i = 0; i < count; i++) {
      const piece = document.createElement("span")
      piece.className = "confetti-piece"
      piece.style.left = `${Math.random() * 100}%`
      piece.style.animationDuration = `${1.8 + Math.random() * 1.8}s`
      piece.style.animationDelay = `${Math.random() * 0.4}s`
      piece.style.transform = `rotate(${Math.random() * 360}deg)`

      layer.appendChild(piece)
    }

    document.body.appendChild(layer)
    window.setTimeout(() => {
      layer.remove()
    }, 4200)
  }
}

export const confettiController = new ConfettiController()