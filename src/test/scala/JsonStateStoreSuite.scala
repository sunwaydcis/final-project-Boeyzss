import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

import munit.FunSuite

import model.AppState
import persistence.JsonStateStore

final class JsonStateStoreSuite extends FunSuite:
  test("JSON state round trip preserves borrowers, loans, schedules, and payments") {
    val temporaryDirectory = Files.createTempDirectory("microfinance-store-test")
    val store = JsonStateStore(temporaryDirectory.resolve("state.json"))
    val originalState = TestFixtures.stateWithLoan

    assertEquals(store.save(originalState), Right(()))
    assertEquals(store.load(), Right(originalState))
  }

  test("missing data file loads as an empty state") {
    val temporaryDirectory = Files.createTempDirectory("microfinance-empty-test")
    val store = JsonStateStore(temporaryDirectory.resolve("missing.json"))

    assertEquals(store.load(), Right(AppState.empty))
  }

  test("corrupt JSON returns a friendly persistence error") {
    val temporaryDirectory = Files.createTempDirectory("microfinance-corrupt-test")
    val statePath = temporaryDirectory.resolve("state.json")
    Files.writeString(statePath, "{not-valid-json", StandardCharsets.UTF_8)
    val result = JsonStateStore(statePath).load()

    assert(result.left.exists(_.message.startsWith("Saved data could not be loaded")))
  }

  test("saved data survives a fresh store instance") {
    val temporaryDirectory = Files.createTempDirectory("microfinance-restart-test")
    val statePath = temporaryDirectory.resolve("state.json")
    val originalState = TestFixtures.stateWithLoan

    assertEquals(JsonStateStore(statePath).save(originalState), Right(()))
    assertEquals(JsonStateStore(statePath).load(), Right(originalState))
  }

  test("bundled fictional sample state is valid JSON for the application") {
    val samplePath = Paths.get("src", "main", "resources", "sample-state.json")
    val loadedState = JsonStateStore(samplePath).load()

    assertEquals(loadedState.map(_.borrowers.size), Right(1))
    assertEquals(loadedState.map(_.loans.size), Right(1))
  }
