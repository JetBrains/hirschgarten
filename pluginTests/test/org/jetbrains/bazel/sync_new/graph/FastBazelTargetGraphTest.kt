package org.jetbrains.bazel.sync_new.graph

import com.intellij.testFramework.junit5.fixture.TestFixtures
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps
import it.unimi.dsi.fastutil.longs.LongSets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.new_sync.storage.storageContextFixture
import org.jetbrains.bazel.sync_new.graph.impl.BazelFastTargetGraph
import org.jetbrains.bazel.sync_new.graph.impl.BazelGenericTargetData
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetEdge
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetTag
import org.jetbrains.bazel.sync_new.graph.impl.BazelTargetVertex
import org.jetbrains.bazel.sync_new.graph.impl.DependencyType
import org.junit.jupiter.api.Test
import java.util.EnumSet

@TestFixtures
internal class FastBazelTargetGraphTest {
  val storageContext = storageContextFixture()
  val tempFixture = tempPathFixture()

  @Test
  fun `test predecessors and successors`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))
    val v4 = graph.addVertex(Label.parse("@//target_4"))
    val v5 = graph.addVertex(Label.parse("@//target_5"))
    graph.addEdge(v1, v2)
    graph.addEdge(v1, v3)
    graph.addEdge(v3, v4)
    graph.addEdge(v3, v5)

    graph.getSuccessors(v1.vertexId).shouldContainExactlyInAnyOrder(v2.vertexId, v3.vertexId)
    graph.getPredecessors(v1.vertexId).shouldBeEmpty()
    graph.getPredecessors(v2.vertexId).shouldContainExactlyInAnyOrder(v1.vertexId)
    graph.getPredecessors(v3.vertexId).shouldContainExactlyInAnyOrder(v1.vertexId)
    graph.getPredecessors(v4.vertexId).shouldContainExactlyInAnyOrder(v3.vertexId)
    graph.getPredecessors(v5.vertexId).shouldContainExactlyInAnyOrder(v3.vertexId)
    graph.getSuccessors(v3.vertexId).shouldContainExactlyInAnyOrder(v4.vertexId, v5.vertexId)
  }

  @Test
  fun `test add and retrieve vertex by id`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val label = Label.parse("@//my:target")
    val vertex = graph.addVertex(label)

    graph.getVertexById(vertex.vertexId).shouldBe(vertex)
  }

  @Test
  fun `test add and retrieve vertex by label`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val label = Label.parse("@//my:target")
    val vertex = graph.addVertex(label)

    graph.getVertexByLabel(label).shouldBe(vertex)
  }

  @Test
  fun `test get vertex id by label`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val label = Label.parse("@//my:target")
    val vertex = graph.addVertex(label)

    graph.getVertexIdByLabel(label).shouldBe(vertex.vertexId)
  }

  @Test
  fun `test get label by vertex id`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val label = Label.parse("@//my:target")
    val vertex = graph.addVertex(label)

    graph.getLabelByVertexId(vertex.vertexId).shouldBe(label)
  }

  @Test
  fun `test get vertex compact by id`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val label = Label.parse("@//my:target")
    val vertex = graph.addVertex(label)

    val compact = graph.getVertexCompactById(vertex.vertexId)
    compact.shouldNotBeNull()
    compact.label.shouldBe(label)
    compact.vertexId.shouldBe(vertex.vertexId)
  }

  @Test
  fun `test get all vertex compacts`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))

    val compacts = graph.getAllVertexCompacts().toList()
    compacts.size.shouldBe(3)
    compacts.map { it.vertexId }.shouldContainExactlyInAnyOrder(v1.vertexId, v2.vertexId, v3.vertexId)
  }

  @Test
  fun `test add and retrieve edge by id`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val edge = graph.addEdge(v1, v2)

    graph.getEdgeById(edge.edgeId).shouldBe(edge)
  }

  @Test
  fun `test get edge between vertices`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val edge = graph.addEdge(v1, v2)

    graph.getEdgeBetween(v1.vertexId, v2.vertexId).shouldBe(edge.edgeId)
  }

  @Test
  fun `test get edge between non-connected vertices`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))

    graph.getEdgeBetween(v1.vertexId, v2.vertexId).shouldBe(EMPTY_ID)
  }

  @Test
  fun `test get outgoing edges`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))
    val edge1 = graph.addEdge(v1, v2)
    val edge2 = graph.addEdge(v1, v3)

    graph.getOutgoingEdges(v1.vertexId).shouldContainExactlyInAnyOrder(edge1.edgeId, edge2.edgeId)
  }

  @Test
  fun `test get incoming edges`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))
    val edge1 = graph.addEdge(v1, v3)
    val edge2 = graph.addEdge(v2, v3)

    graph.getIncomingEdges(v3.vertexId).shouldContainExactlyInAnyOrder(edge1.edgeId, edge2.edgeId)
  }

  @Test
  fun `test vertices sequence`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))

    val vertices = graph.vertices.toList()
    vertices.size.shouldBe(3)
    vertices.shouldContain(v1)
    vertices.shouldContain(v2)
    vertices.shouldContain(v3)
  }

  @Test
  fun `test edges sequence`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))
    val edge1 = graph.addEdge(v1, v2)
    val edge2 = graph.addEdge(v2, v3)

    val edges = graph.edges.toList()
    edges.size.shouldBe(2)
    edges.shouldContain(edge1)
    edges.shouldContain(edge2)
  }

  @Test
  fun `test remove vertex by id`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))
    graph.addEdge(v1, v2)
    graph.addEdge(v2, v3)

    val removed = graph.removeVertexById(v2.vertexId)
    removed.shouldBe(v2)

    graph.getVertexById(v2.vertexId).shouldBeNull()
    graph.getVertexByLabel(v2.label).shouldBeNull()
    graph.getSuccessors(v1.vertexId).shouldBeEmpty()
    graph.getPredecessors(v3.vertexId).shouldBeEmpty()
  }

  @Test
  fun `test remove vertex removes associated edges`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))
    val edge1 = graph.addEdge(v1, v2)
    val edge2 = graph.addEdge(v2, v3)

    graph.removeVertexById(v2.vertexId)

    graph.getEdgeById(edge1.edgeId).shouldBeNull()
    graph.getEdgeById(edge2.edgeId).shouldBeNull()
  }

  @Test
  fun `test remove non-existent vertex returns null`() {
    val graph = BazelFastTargetGraph(storageContext.get())

    graph.removeVertexById(999).shouldBeNull()
  }

  @Test
  fun `test remove edge by id`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val edge = graph.addEdge(v1, v2)

    val removed = graph.removeEdgeById(edge.edgeId)
    removed.shouldBe(edge)

    graph.getEdgeById(edge.edgeId).shouldBeNull()
    graph.getSuccessors(v1.vertexId).shouldBeEmpty()
    graph.getPredecessors(v2.vertexId).shouldBeEmpty()
    graph.getEdgeBetween(v1.vertexId, v2.vertexId).shouldBe(EMPTY_ID)
  }

  @Test
  fun `test remove non-existent edge returns null`() {
    val graph = BazelFastTargetGraph(storageContext.get())

    graph.removeEdgeById(999).shouldBeNull()
  }

  @Test
  fun `test get all vertex ids`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    val v3 = graph.addVertex(Label.parse("@//target_3"))

    val vertexIds = graph.getAllVertexIds()
    vertexIds.size.shouldBe(3)
    vertexIds.shouldContain(v1.vertexId)
    vertexIds.shouldContain(v2.vertexId)
    vertexIds.shouldContain(v3.vertexId)
  }

  @Test
  fun `test next vertex id increments`() {
    val graph = BazelFastTargetGraph(storageContext.get())

    val id1 = graph.getNextVertexId()
    val id2 = graph.getNextVertexId()
    val id3 = graph.getNextVertexId()

    id2.shouldBe(id1 + 1)
    id3.shouldBe(id2 + 1)
  }

  @Test
  fun `test next edge id increments`() {
    val graph = BazelFastTargetGraph(storageContext.get())

    val id1 = graph.getNextEdgeId()
    val id2 = graph.getNextEdgeId()
    val id3 = graph.getNextEdgeId()

    id2.shouldBe(id1 + 1)
    id3.shouldBe(id2 + 1)
  }

  @Test
  fun `test clear graph`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))
    graph.addEdge(v1, v2)

    graph.clear()

    graph.vertices.toList().shouldBeEmpty()
    graph.edges.toList().shouldBeEmpty()
    graph.getAllVertexIds().shouldBeEmpty()
  }

  @Test
  fun `test empty graph successors and predecessors`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val vertex = graph.addVertex(Label.parse("@//target"))

    graph.getSuccessors(vertex.vertexId).shouldBeEmpty()
    graph.getPredecessors(vertex.vertexId).shouldBeEmpty()
  }

  @Test
  fun `test multiple edges between same vertices`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val v1 = graph.addVertex(Label.parse("@//target_1"))
    val v2 = graph.addVertex(Label.parse("@//target_2"))

    val edge1 = BazelTargetEdge(
      edgeId = graph.getNextEdgeId(),
      from = v1.vertexId,
      to = v2.vertexId,
      type = DependencyType.COMPILE,
    )
    graph.addEdge(edge1)

    val edge2 = BazelTargetEdge(
      edgeId = graph.getNextEdgeId(),
      from = v1.vertexId,
      to = v2.vertexId,
      type = DependencyType.RUNTIME,
    )
    graph.addEdge(edge2)

    graph.getSuccessors(v1.vertexId).size.shouldBe(2)
    graph.getPredecessors(v2.vertexId).size.shouldBe(2)
  }

  @Test
  fun `test executable target in compact`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val label = Label.parse("@//my:binary")
    val vertex = BazelTargetVertex(
      vertexId = graph.getNextVertexId(),
      label = label,
      genericData = BazelGenericTargetData(
        tags = EnumSet.of(BazelTargetTag.EXECUTABLE),
        directDependencies = emptyList(),
        sources = emptyList(),
        resources = emptyList(),
        isUniverseTarget = true,
      ),
      languageTags = LongSets.EMPTY_SET,
      targetData = Long2ObjectMaps.emptyMap(),
      baseDirectory = tempFixture.get(),
      kind = "binary",
    )
    graph.addVertex(vertex)

    val compact = graph.getVertexCompactById(vertex.vertexId)
    compact.shouldNotBeNull()
    compact.isExecutable.shouldBe(true)
  }

  @Test
  fun `test non-executable target in compact`() {
    val graph = BazelFastTargetGraph(storageContext.get())
    val label = Label.parse("@//my:library")
    val vertex = graph.addVertex(label)

    val compact = graph.getVertexCompactById(vertex.vertexId)
    compact.shouldNotBeNull()
    compact.isExecutable.shouldBe(false)
  }

  @Test
  fun `test get vertex by non-existent label`() {
    val graph = BazelFastTargetGraph(storageContext.get())

    graph.getVertexByLabel(Label.parse("@//non:existent")).shouldBeNull()
  }

  @Test
  fun `test get label by non-existent vertex id`() {
    val graph = BazelFastTargetGraph(storageContext.get())

    graph.getLabelByVertexId(999).shouldBeNull()
  }

  private fun BazelFastTargetGraph.addVertex(label: Label): BazelTargetVertex {
    val vertex = BazelTargetVertex(
      vertexId = getNextVertexId(),
      label = label,
      genericData = BazelGenericTargetData(
        tags = EnumSet.noneOf(BazelTargetTag::class.java),
        directDependencies = emptyList(),
        sources = emptyList(),
        resources = emptyList(),
        isUniverseTarget = true,
      ),
      languageTags = LongSets.EMPTY_SET,
      targetData = Long2ObjectMaps.emptyMap(),
      baseDirectory = tempFixture.get(),
      kind = "unknown",
    )
    addVertex(vertex)
    return vertex
  }

  private fun BazelFastTargetGraph.addEdge(from: BazelTargetVertex, to: BazelTargetVertex): BazelTargetEdge {
    val edge = BazelTargetEdge(
      edgeId = getNextEdgeId(),
      from = from.vertexId,
      to = to.vertexId,
      type = DependencyType.COMPILE,
    )
    addEdge(edge)
    return edge
  }
}
